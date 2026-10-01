package service;

import model.TelemetryData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesador de mensajes del protocolo de telemetría IoT sobre UDP.
 * 
 * Formato de mensajes recibidos:
 *   - Registro de telemetría: "DEVICE_ID;SENSOR_TYPE;VALUE" (ej. "sensor-01;TEMP;25.5")
 *   - Consulta de estado:      "STATUS;DEVICE_ID" (ej. "STATUS;sensor-01")
 */
public class TelemetryProcessor {

    private final Map<String, TelemetryData> lastReadings = new ConcurrentHashMap<>();

    /**
     * Procesa un mensaje de texto recibido por UDP y devuelve la respuesta
     * correspondiente según las reglas del protocolo de telemetría.
     * 
     * @param rawMessage Mensaje en texto plano recibido en el datagrama UDP.
     * @return Respuesta que será enviada de regreso al cliente emisor.
     */
    public String process(String rawMessage) {
        // TODO Paso 1.1: Validar que el mensaje no sea nulo ni esté vacío (usar trim()).
        // Si no es válido, retornar "ERROR;INVALID_FORMAT".
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        String message = rawMessage.trim();
        // TODO Paso 1.2: Separar el mensaje usando el delimitador ";".
        // Si el arreglo resultante está vacío, retornar "ERROR;INVALID_FORMAT".
        String[] parts = message.split(";");
        if(parts.length == 0){
            return "ERROR;INVALID_FORMAT";
        }
        // TODO Paso 1.3: Si la primera parte es "STATUS" (ignorar mayúsculas/minúsculas):
        //   - Validar que tenga exactamente 2 partes y que el DEVICE_ID no esté en blanco.
        //   - Si no cumple, retornar "ERROR;INVALID_FORMAT".
        //   - Buscar en 'lastReadings' por DEVICE_ID.
        //   - Si no existe, retornar "ERROR;DEVICE_NOT_FOUND".
        //   - Si existe, retornar "STATUS_OK;DEVICE_ID;SENSOR_TYPE;VALUE".
        if(parts[0].equalsIgnoreCase("STATUS")){
            if(parts.length != 2 || parts[1].trim().isEmpty()){
                return "ERROR;INVALID_FORMAT";
            }
            else if(!lastReadings.containsKey(parts[1])){
                return "ERROR;DEVICE_NOT_FOUND";
            }
            TelemetryData data = lastReadings.get(parts[1]);
            return "STATUS_OK;"+data.getDeviceId()+";"+data.getSensorType()+";"+data.getValue();
        }
        // TODO Paso 1.4: Validar formato de telemetría: deben ser exactamente 3 partes no vacías:
        // [0] = deviceId, [1] = sensorType, [2] = valueStr.
        // Si no cumple, retornar "ERROR;INVALID_FORMAT".
        // Intentar convertir valueStr a double (Double.parseDouble).
        // Si falla con NumberFormatException, retornar "ERROR;INVALID_FORMAT".
        if (parts.length != 3 ||
                parts[0].trim().isEmpty() ||
                parts[1].trim().isEmpty() ||
                parts[2].trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        String deviceId = parts[0];
        String sensorType = parts[1];
        double value;
        try {
            value = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return "ERROR;INVALID_FORMAT";
        }
        // TODO Paso 1.5: Guardar la lectura válida en 'lastReadings':
        lastReadings.put(deviceId, new TelemetryData(deviceId, sensorType, value));

        // TODO Paso 1.6: Validar sensorType (TEMP, HUMIDITY, BATTERY) y evaluar rangos:
        // - TEMP:
        //     valor > 40.0 -> "ALERT;HIGH_TEMPERATURE;" + value
        //     valor < 0.0  -> "ALERT;FREEZING_TEMPERATURE;" + value
        //     otro         -> "OK;TEMP_RECORDED;" + value
        // - HUMIDITY:
        //     valor > 90.0 -> "ALERT;HIGH_HUMIDITY;" + value
        //     valor < 20.0 -> "ALERT;LOW_HUMIDITY;" + value
        //     otro         -> "OK;HUMIDITY_RECORDED;" + value
        // - BATTERY:
        //     valor < 20.0 -> "ALERT;LOW_BATTERY;" + value
        //     otro         -> "OK;BATTERY_OK;" + value
        // - Cualquier otro sensorType:
        //     retornar "ERROR;UNKNOWN_SENSOR_TYPE"
        if(sensorType.equalsIgnoreCase("TEMP")){
            if(value > 40.0){
                return "ALERT;HIGH_TEMPERATURE;" + value;
            }else if(value < 0.0){
                return "ALERT;FREEZING_TEMPERATURE;" + value;
            } else {
                return "OK;TEMP_RECORDED;" + value;
            }
        }
        if(sensorType.equalsIgnoreCase("HUMIDITY")){
            if(value > 90.0){
                return "ALERT;HIGH_HUMIDITY;" + value;
            } else if(value < 20){
                return "ALERT;LOW_HUMIDITY;" + value;
            } else{
                return "OK;HUMIDITY_RECORDED;" + value;
            }
        }
        if(sensorType.equalsIgnoreCase(("BATTERY"))){
            if(value<20){
                return "ALERT;LOW_BATTERY;" + value;
            }else{
                return "OK;BATTERY_OK;" + value;
            }
        }else {
            return "ERROR;UNKNOWN_SENSOR_TYPE";
        }
    }

    public Map<String, TelemetryData> getLastReadings() {
        return lastReadings;
    }

    public void clear() {
        lastReadings.clear();
    }
}
