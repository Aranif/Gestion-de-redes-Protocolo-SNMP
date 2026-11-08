# 🚀 Sistema de Monitorización y Mitigación Automática SNMP con Bot de Discord

Este proyecto implementa un sistema híbrido de seguridad y gestión de redes distribuido en dos componentes principales: un **Monitor y Analizador de Tráfico en Java** y un **Bot de Control y Alertas Asíncrono en Python**. El objetivo es monitorizar el tráfico de interfaces de red mediante SNMPv2c, detectar anomalías o ataques de sobrecarga (DoS) que superen un umbral predefinido, persistir los estados en una base de datos MySQL y permitir la mitigación manual o el aislamiento de interfaces a través de comandos interactivos de Discord.

---

## 🛠️ Herramientas de Desarrollo y Co-Creación con IA
Este proyecto ha sido desarrollado utilizando un enfoque de **Ingeniería de Software Asistida**. Queremos hacer constar de forma explícita que se ha empleado la **Inteligencia Artificial (IA)** como una herramienta de desarrollo:
* **Copiloto técnico:** La IA se ha utilizado para agilizar la escritura de estructuras repetitivas, adaptar la sintaxis asíncrona a las últimas actualizaciones de librerías (`pysnmp-lextudio`) y optimizar queries SQL. Además de poner bonitos los mensajes que devuelven los ejecutables.
* **Mérito del autor:** El uso de IA no resta valor ni complejidad al proyecto; al contrario, actúa como un acelerador. El diseño de la topología (routing de la maqueta física), la lógica del cálculo de diferencias de tráfico (octetos), el modelado de la base de datos y la orquestación integral de las tres capas técnicas han sido ideados y estructurados completamente por el autor. La IA se trata aquí como lo que es: una herramienta avanzada más en el cinturón de un administrador de redes moderno. Recomiendo: antes de modificar y añadir cosasqeu quieres hacer y preguntar por los objetos que hay disponible luego indagar en contenidos oficiales sobre esos objetos.

---

## 📐 Arquitectura del Sistema y Librerías Clave

El sistema se divide en tres capas interconectadas:
1. **Capa de Monitorización (Java):** Un hilo demonio consulta periódicamente los OIDs activos utilizando **SNMP4J**. Calcula el tráfico diferencial en base a los octetos recibidos y analiza si se ha superado el `threshold` numérico configurado.
2. **Capa de Persistencia (MySQL):** Actúa como el puente o "middleware" de comunicación entre Java y Python. Almacena las configuraciones de los OID y los flags de alerta (`alerta_enviada`).
3. **Capa de Notificación y Control (Python):** Un bucle asíncrono monitoriza la base de datos y envía alertas enriquecidas a un canal de Discord. Permite el apagado administrativo inmediato de interfaces usando comandos asíncronos.

### Librerías Importantes
* **`org.snmp4j` (Java):** Core para la creación de PDUs de tipo `GET` y `SET`, mapeos de transporte UDP (`DefaultUdpTransportMapping`) y gestión de targets SNMP.
* **`discord.ext.tasks` y `commands` (Python / Discord.py):** Permite la creación del bot, definición de comandos avanzados con tipado dinámico y la ejecución de loops asíncronos (`@tasks.loop`) sin bloquear el procesamiento de mensajes de Discord.
* **`pysnmp.hlapi.asyncio` (Python / PySNMP):** Emplea las últimas especificaciones asíncronas para inyectar comandos `set_cmd` de manera no bloqueante nativa, usando factorías de transporte UDP virtuales (`UdpTransportTarget.create`). La idea esque partiendo de este codigo se puede hacer muchas más cosas con los objetos que se guardan en las MIBs.

---

## 🚀 Guía de Instalación, Compilación y Ejecución

### 1. Requisitos Previos
Asegúrate de tener un servidor MySQL corriendo localmente con una base de datos llamada `redes_db` y una tabla `monitoreo` estructurada con las columnas: `id`, `nombre_oid`, `valor`, `threshold`, `OID`, y `alerta_enviada`.

---

### 2. Componente Java (Monitorización)
El proyecto utiliza **Maven (mvn)** para la gestión de dependencias y construcción.

#### Compilación con Maven:
Abra una terminal en la raíz del proyecto Java (donde se encuentre el archivo `pom.xml`) y ejecute:
```bash
mvn clean compile
```
#### Ejecución con Maven (se tiene que configurar un pom.xml con las dependencias antes, cualquier IA lo hace en un momento):
```bash
# Opción con Maven
mvn exec:java -Dexec.mainClass="bot.controlador.Main"

# Opción directa con Java
java -jar target/tu-proyecto-monitor-1.0.jar
```
### 3. Creacion e instalación del Entorno virtual 
Nos sirve para no corromper la maquina en la que nos encontramos instalando librerias a lo loco para nuestros codigos.
```bash
# Si no tiene instalado los entornos virutales de python
sudo apt install python3-venv  # (Solo si no está instalado en tu sistema Linux)
python3 -m venv venv
# Activar entorno virtual
source venv/bin/activate
# Instalar dependencias necesarias dentro del entorno
pip install --upgrade pip
pip install discord.py pymysql python-dotenv pysnmp-lextudio

```
### 4. Configuración de variables de entornos
Como para acceder a la base de datos tenemos contraseñas y cosas privadas al igual que en el chat de discord usamos un .env.
```bash
DISCORD_TOKEN=tu_token_secreto_de_discord
ID_CANAL=click_derecho_encanal_discord
DB_USER=root
DB_PASSWORD=tu_contraseña_mysql
DB_NAME=redes_db
