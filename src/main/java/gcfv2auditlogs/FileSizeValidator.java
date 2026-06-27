package gcfv2auditlogs;

import com.google.cloud.functions.CloudEventsFunction;
import io.cloudevents.CloudEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class FileSizeValidator implements CloudEventsFunction {
    
    private static final Logger logger = Logger.getLogger(FileSizeValidator.class.getName());
    // Límite de tamaño: 5 MB en bytes
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; 

    @Override
    public void accept(CloudEvent event) {
        // 1. Validar que el evento tenga datos
        if (event.getData() == null) {
            logger.warning("El CloudEvent no contiene datos.");
            return;
        }

        try {
            // 2. Extraer y parsear el payload JSON de Cloud Storage
            String cloudEventData = new String(event.getData().toBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonObject storageObject = gson.fromJson(cloudEventData, JsonObject.class);

            // 3. Obtener la metadata del archivo
            logger.info(storageObject.toString());
            String fileName = storageObject.has("name") ? storageObject.get("name").getAsString() : null;
            String bucketName = storageObject.has("bucket") ? storageObject.get("bucket").getAsString(): null;
            long fileSize = storageObject.has("size") ? storageObject.get("size").getAsLong(): 0L;

            logger.info(String.format("Analizando archivo: %s en bucket: %s (Tamaño: %d bytes)", 
                                      fileName, bucketName, fileSize));

            // 4. Lógica de validación
            if (fileSize > MAX_SIZE_BYTES) {
                logger.warning(String.format("❌ ALERTA: El archivo '%s' excede el límite permitido. " +
                                             "Tamaño actual: %d bytes. Límite: %d bytes.", 
                                             fileName, fileSize, MAX_SIZE_BYTES));
                
                // NOTA: Si deseas borrar el archivo automáticamente aquí, 
                // necesitarías importar la librería 'Google Cloud-storage'
                // y usar el cliente de Storage para eliminar el objeto.
                
            } else {
                logger.info(String.format("✅ ÉXITO: El archivo '%s' tiene un tamaño válido.", fileName));
            }

        } catch (Exception e) {
            logger.severe("Error al procesar el evento: " + e.getMessage());
        }
    }
}