import json
import sqlite3
import time
import argparse

from contextlib import closing
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
from uuid import UUID

DATABASE = Path(__file__).parent / "recordatorios_servidor.db"

parser = argparse.ArgumentParser()
parser.add_argument("--puerto", type=int, default=8000)
parser.add_argument(
    "--modo",
    choices=["normal", "servidor", "invalido", "timeout"],
    default="normal"
)
args = parser.parse_args()


def inicializar_base():
    with closing(sqlite3.connect(DATABASE)) as conexion:
        with conexion:
            conexion.execute("""
                CREATE TABLE IF NOT EXISTS recordatorios (
                    clave TEXT PRIMARY KEY,
                    version INTEGER NOT NULL,
                    datos TEXT NOT NULL
                )
            """)


def datos_validos(datos, clave):
    if not isinstance(datos, dict):
        return False

    try:
        UUID(clave)
    except ValueError:
        return False

    campos = [
        "claveSincronizacion",
        "titulo",
        "categoria",
        "prioridad",
        "estado"
    ]

    if not all(isinstance(datos.get(c), str) for c in campos):
        return False

    if datos["claveSincronizacion"] != clave:
        return False

    if len(datos["titulo"].strip()) < 4:
        return False

    if datos["prioridad"] not in ["ALTA", "MEDIA", "BAJA"]:
        return False

    if datos["estado"] not in ["PENDIENTE", "COMPLETADO"]:
        return False

    for campo in ["fechaCreacion", "fechaModificacion", "versionLocal"]:
        if type(datos.get(campo)) is not int:
            return False
        if datos[campo] <= 0:
            return False

    if type(datos.get("eliminado")) is not bool:
        return False

    for campo in ["descripcion", "ubicacion", "aviso"]:
        valor = datos.get(campo)
        if valor is not None and not isinstance(valor, str):
            return False

    if len(datos.get("descripcion") or "") > 200:
        return False

    return True


class ServidorRecordatorios(BaseHTTPRequestHandler):

    wbufsize = 65536
    disable_nagle_algorithm = True

    def responder(self, codigo, datos):
        contenido = json.dumps(
            datos, ensure_ascii=False
        ).encode("utf-8")

        try:
            self.send_response(codigo)
            self.send_header(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            self.send_header("Content-Length", str(len(contenido)))
            self.end_headers()
            self.wfile.write(contenido)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def simular_error(self):
        if args.modo == "servidor":
            self.responder(500, {"error": "Error simulado"})
            return True

        if args.modo == "invalido":
            self.responder(400, {"error": "Solicitud invalida"})
            return True

        if args.modo == "timeout":
            time.sleep(20)

        return False

    def do_GET(self):
        ruta = urlsplit(self.path).path

        if ruta == "/salud":
            self.responder(200, {
                "estado": "OK",
                "modo": args.modo
            })
            return

        if ruta != "/recordatorios":
            self.responder(404, {"error": "Ruta no encontrada"})
            return

        if self.simular_error():
            return

        try:
            with closing(sqlite3.connect(DATABASE)) as conexion:
                filas = conexion.execute(
                    "SELECT datos FROM recordatorios ORDER BY clave"
                ).fetchall()

                self.responder(
                    200,
                    [json.loads(fila[0]) for fila in filas]
                )

        except sqlite3.Error:
            self.responder(500, {"error": "Error de base de datos"})

    def do_PUT(self):
        ruta = urlsplit(self.path).path
        partes = ruta.strip("/").split("/")

        if len(partes) != 2 or partes[0] != "recordatorios":
            self.responder(404, {"error": "Ruta no encontrada"})
            return

        if self.simular_error():
            return

        clave = partes[1]

        try:
            longitud = int(self.headers.get("Content-Length", "0"))

            if longitud <= 0 or longitud > 65536:
                raise ValueError("Longitud incorrecta")

            datos = json.loads(
                self.rfile.read(longitud).decode("utf-8")
            )

        except (ValueError, UnicodeError):
            self.responder(400, {"error": "JSON invalido"})
            return

        if not datos_validos(datos, clave):
            self.responder(400, {"error": "Datos invalidos"})
            return

        version = datos["versionLocal"]

        try:
            with closing(sqlite3.connect(DATABASE)) as conexion:
                with conexion:
                    conexion.execute("BEGIN IMMEDIATE")

                    existente = conexion.execute(
                        "SELECT version, datos FROM recordatorios WHERE clave = ?",
                        (clave,)
                    ).fetchone()

                    if existente and (
                        existente[0] > version or
                        (
                            existente[0] == version and
                            json.loads(existente[1]) != datos
                        )
                    ):
                        self.responder(409, {
                            "error": "Conflicto de version"
                        })
                        return

                    conexion.execute("""
                        INSERT INTO recordatorios
                            (clave, version, datos)
                        VALUES (?, ?, ?)
                        ON CONFLICT(clave) DO UPDATE SET
                            version = excluded.version,
                            datos = excluded.datos
                    """, (
                        clave,
                        version,
                        json.dumps(datos, ensure_ascii=False)
                    ))

            self.responder(200, datos)

        except sqlite3.Error:
            self.responder(500, {"error": "Error de guardado"})


if __name__ == "__main__":
    inicializar_base()

    servidor = ThreadingHTTPServer(
        ("127.0.0.1", args.puerto),
        ServidorRecordatorios
    )

    print("======================================")
    print(" SERVIDOR DE RECORDATORIOS")
    print("======================================")
    print(f"URL: http://127.0.0.1:{args.puerto}")
    print(f"Modo: {args.modo}")
    print("Presiona Ctrl+C para detenerlo")
    print("======================================")

    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido")
    finally:
        servidor.server_close()
