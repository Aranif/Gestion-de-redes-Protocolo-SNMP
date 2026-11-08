import discord
from discord.ext import tasks, commands
import pymysql # Librería compatible
import os
import asyncio 
from pathlib import Path
from dotenv import load_dotenv
import pysnmp.hlapi.asyncio as snmp_async
from pysnmp.proto.rfc1902 import Integer
# --- CONFIGURACIÓN ---
ruta_al_env = Path(__file__).resolve().parent.parent.parent.parent / "resources" / ".env"
load_dotenv(dotenv_path=ruta_al_env)

TOKEN_DISCORD = os.getenv("DISCORD_TOKEN")
ID_CANAL = int(os.getenv("ID_CANAL"))

class Mibotof(commands.Bot):
    def __init__(self): 
        intents = discord.Intents.default()
        intents.message_content = True
        super().__init__(command_prefix="haz ", intents=intents)
        self.__canal = None  
        self.__lista = []
    
    async def setup_hook(self):
        self.verBbdd.start()
    async def on_ready(self):
        print(f"Bot online: {self.user}")
        self.__canal = self.get_channel(ID_CANAL)
        await self.__canal.send("🚀 Monitor SNMP iniciado y listo para alertar.")
    
    def consultar_db(self):
        print("🔌 Conectando a MySQL (PyMySQL)...")
        resultados = []
        db = None
        try:
            db =  pymysql.connect(
                host="127.0.0.1",
                user=os.getenv("DB_USER"),
                passwd=os.getenv("DB_PASSWORD"),
                database=os.getenv("DB_NAME"),
                cursorclass=pymysql.cursors.DictCursor
            )
            with db.cursor() as cursor:
                cursor.execute("SELECT COUNT(*) as total FROM monitoreo")
                total = cursor.fetchone()['total']
                if(total != 0):
                    print(f"📊 Filas totales en la tabla: {total}")
                    query = "SELECT id, nombre_oid, valor FROM monitoreo WHERE alerta_enviada = TRUE"
                    cursor.execute(query) 
                    resultados = cursor.fetchall()
                else:
                    print("📊 La tabla está vacía.")
        except Exception as e:
            print(f"❌ Error al conectar o consultar la base de datos: {e}")
        finally:
            if db:
                    db.close()
        return resultados


    @tasks.loop(seconds=8)
    async def verBbdd(self):
        await self.wait_until_ready()

        filas = await asyncio.to_thread(self.consultar_db)
        if not filas:
            print("ℹ️ No hay filas de alerta para procesar en la base de datos.")
            return
        contador = 0
        for fila in filas:
            id_alerta = fila['id']
            nombre_oid = fila['nombre_oid']
            valor_db = fila['valor']
            contador += 1;
            oid_actual = {
                "id": id_alerta,
                "nombre_oid": nombre_oid,
                "valor": valor_db,
                "contador": contador
            }
            # --- MODIFICAR EN EL BUCLE for fila in filas ---
            if "TRÁFICO" in nombre_oid:
                await self.__canal.send(f"⚠️ **ALERTA DE SOBRECARGA (DoS)** ⚠️\n"
                            f"La interfaz {nombre_oid} ha superado el límite de tráfico permitido.")
            
            
            # Guardar ID en la lista si no existe ya
            if oid_actual not in self.__lista:
                self.__lista.append(oid_actual)
                await self.__canal.send(f"🔔 ¡He encontrado una alerta nueva! objeto: {nombre_oid}, Valor: {valor_db}")
                if(contador == 4):
                    await self.__canal.send(f"⚠️ ¡El objeto con nombre {nombre_oid} lleva más de 2 minutos superando el umbral!")
            

bot = Mibotof()
@bot.command()
async def bloquea_interfaz(ctx, num_interfaz: int):
    oid_destino = f"1.3.6.1.2.1.2.2.1.7.{num_interfaz}"
    
    try:
      
        transporte = await snmp_async.UdpTransportTarget.create(("127.0.0.1", 161))
        errorIndication, errorStatus, errorIndex, varBinds = await snmp_async.set_cmd(
            snmp_async.SnmpEngine(),
            snmp_async.CommunityData("private", mpModel=0),
            transporte, #
            snmp_async.ContextData(),
            snmp_async.ObjectType(snmp_async.ObjectIdentity(oid_destino), Integer(2))
        )

        if errorIndication:
            await ctx.send(f"❌ Error de red: {errorIndication}")
        elif errorStatus:
            await ctx.send(f"❌ Error SNMP: {errorStatus.prettyPrint()}")
        else:
            await ctx.send(f"🛑 Interfaz **{num_interfaz}** apagada con éxito.")

    except Exception as e:
        await ctx.send(f"⚠️ Error: {e}")
        print(f"DEBUG: {e}")
        
@bot.command()
async def activa_interfaz(ctx, num_interfaz: int):
    oid_destino = f"1.3.6.1.2.1.2.2.1.7.{num_interfaz}"
    
    try:
      
        transporte = await snmp_async.UdpTransportTarget.create(("127.0.0.1", 161))
        errorIndication, errorStatus, errorIndex, varBinds = await snmp_async.set_cmd(
            snmp_async.SnmpEngine(),
            snmp_async.CommunityData("private", mpModel=0),
            transporte, #
            snmp_async.ContextData(),
            snmp_async.ObjectType(snmp_async.ObjectIdentity(oid_destino), Integer(1))
        )

        if errorIndication:
            await ctx.send(f"❌ Error de red: {errorIndication}")
        elif errorStatus:
            await ctx.send(f"❌ Error SNMP: {errorStatus.prettyPrint()}")
        else:
            await ctx.send(f"✅ Interfaz **{num_interfaz}** activada con éxito.")

    except Exception as e:
        await ctx.send(f"⚠️ Error: {e}")
        print(f"DEBUG: {e}")

bot.run(TOKEN_DISCORD)
