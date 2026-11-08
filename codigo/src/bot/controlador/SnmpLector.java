package bot.controlador;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping; // Este faltaba
import io.github.cdimascio.dotenv.Dotenv;

public class SnmpLector {
    private static Dotenv dotenv = Dotenv.load();
    private String ipRouter = "127.0.0.1"; //Esa IP es la local, se debe cambiar a la del router que se quiere controlar
    private String community = "public";
    private String valor = "";

    public String leerOid(String oid) throws Exception{
        
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();

        CommunityTarget<UdpAddress> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ipRouter + "/161"));
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        try (Snmp snmp = new Snmp(transport)) {
            ResponseEvent<UdpAddress> event = snmp.send(pdu, target);
            if(event.getResponse() != null){
                valor =  event.getResponse().get(0).getVariable().toString();
            }
        }
        return valor;
    }
    public void bloquearPuerto(int interfaz) throws Exception{
          TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        
        CommunityTarget<UdpAddress> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ipRouter + "/161"));
        target.setVersion(SnmpConstants.version2c);
        
        PDU pdu = new PDU();
        OID oid = new OID("1.3.6.1.2.1.2.2.1.7."+interfaz);
        pdu.add(new VariableBinding(oid, new Integer32(2)));
        pdu.setType(PDU.SET);
        try (Snmp snmp = new Snmp(transport)) {
            ResponseEvent<UdpAddress> event = snmp.send(pdu, target);
            if(event.getResponse() != null){
                System.out.println("Apagada interfaz: "+interfaz);
            }else{
              System.out.println("No se recibió respuesta al apagar interfaz"); 
            }
        }
    
    }
}
