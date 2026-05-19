package bot.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import bot.modelo.ObjetosDAO;

public class Main {
    // Variables estáticas para que el hilo pueda verlas
    private static boolean leyendo = true; 

    public static void main(String[] args) throws Exception {
        ObjetosDAO dao = new ObjetosDAO();
        SnmpLector lector = new SnmpLector();
        Scanner sc = new Scanner(System.in);

        // --- HILO DE LECTURA AUTOMÁTICA ---
        Thread hiloLectura = new Thread(() -> {
            while (true) {
                if (leyendo) {
                    try {
                        List<String> oidsActivos = dao.obtenerOidsActivos();
                        for (String oid : oidsActivos) {
                            String valorLeido = lector.leerOid(oid);
                            dao.actualizarDatoYVerificarAlerta(oid, valorLeido);
                        }
                        // Espera 30 segundos antes de la siguiente lectura
                        Thread.sleep(8000); 
                    } catch (Exception e) {
                        System.out.println("⚠️ Error en lectura automática: " + e.getMessage());
                    }
                } else {
                    // Si está en pausa, espera un poco para no saturar la CPU
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
            }
        });
        hiloLectura.setDaemon(true); // Para que se cierre al cerrar el programa
        hiloLectura.start();

        // --- MENÚ PRINCIPAL (HILO EN PRIMER PLANO) ---
        while (true) {
            System.out.println("\n--- MONITOR SNMP ACTIVO ---");
            System.out.println("Estado actual: " + (leyendo ? "📡 LEYENDO" : "⏸️ PAUSADO"));
            System.out.println("1. Insertar nuevo objeto");
            System.out.println("2. Eliminar OID");
            System.out.println("3. " + (leyendo ? "Pausar" : "Reanudar") + " lectura");
            System.out.println("4. Salir");
            System.out.print("Selecciona una opción: ");

            int opcion = sc.nextInt();
            sc.nextLine(); 

            switch (opcion) {
                case 1:
                    System.out.println("Introduce el nombre del OID:");
                    String nombre = sc.nextLine();
                    System.out.println("Introduce el umbral:");
                    String umbral = sc.nextLine();
                    System.out.println("Introduce el OID específico");
                    String oid = sc.nextLine();
                    dao.insertarNuevoOid(nombre, umbral, oid);
                    break;
                case 2:
                    System.out.println("Introduce el OID a eliminar:");
                    String oidBorrar = sc.nextLine();
                    dao.eliminarOid(oidBorrar);
                    System.out.println("🗑️ OID eliminado.");
                    break;
                case 3:
                    leyendo = !leyendo; // Cambia de true a false o viceversa
                    System.out.println(leyendo ? "▶️ Reanudando..." : "⏸️ Lectura pausada.");
                    break;
                case 4:
                    System.out.println("Saliendo...");
                    sc.close();
                    System.exit(0);
                default:
                    System.out.println("Opción no válida. Intenta de nuevo.");
                    break;
            }
        }
    }
}
