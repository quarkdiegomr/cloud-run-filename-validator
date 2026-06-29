package imageValidator;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.events.cloud.storage.v1.StorageObjectData;
import com.google.protobuf.util.JsonFormat;
import io.cloudevents.CloudEvent;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class FileSizeValidator implements CloudEventsFunction {

    private static final Logger logger = Logger.getLogger(FileSizeValidator.class.getName());
    // Límite de tamaño: 2 MB en bytes
    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;


    Storage storage = StorageOptions.getDefaultInstance().getService();


    @Override
    public void accept(CloudEvent event) {
        // 1. Validar que el evento tenga datos
        if (event.getData() == null) {
            logger.warning("El CloudEvent no contiene datos.");
            return;
        }

        try {

            // 1. Extraer el payload JSON interno del CloudEvent
            String eventDataJson = new String(event.getData().toBytes(), StandardCharsets.UTF_8);

            // 2. Inicializar el Builder oficial y parsear el JSON ignorando campos desconocidos
            StorageObjectData.Builder builder = StorageObjectData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(eventDataJson, builder);
            StorageObjectData storageData = builder.build();

            String bucketName = storageData.getBucket();
            String objectName = storageData.getName();
            Blob file = storage.get(BlobId.of(bucketName, objectName));
            String contentType = SecureFileAnalyzer.getRealContentType(bucketName, objectName);

            logger.info(String.format("Analizando archivo: %s en bucket: %s (Tamaño: %d bytes). Max size: %d bytes.",
                    objectName, bucketName, file.getSize(), MAX_SIZE_BYTES));

            // Lógica de validación
            String allowedContentTypes = "image/jpeg, image/png, image/gif, image/heic";
            if (file.getSize() > MAX_SIZE_BYTES && !contentType.contains(allowedContentTypes)) {
                logger.warning(String.format("❌ ALERTA: El archivo '%s' excede el límite permitido. " +
                                "Tamaño actual: %d bytes. Límite: %d bytes.",
                        objectName, file.getSize(), MAX_SIZE_BYTES));
                deleteFileFromBucket(file);

            } else if(isValidPhoto(file)) {
                logger.info(String.format("✅ ÉXITO: El archivo '%s' es una foto valida.", objectName));
            }

        } catch (Exception e) {
            logger.severe("Error al procesar el evento: " + e.getMessage());
        }
    }

    private boolean validateFileName(Blob file, String regex) {
        return file.getName().toLowerCase().matches(regex.toLowerCase());
    }

    private boolean isValidPhoto(Blob file) {
        String regex = ".*edit-me.*"; // edit this regex to match your file naming convention
        if (!validateFileName(file, regex)) {
            boolean deleted = deleteFileFromBucket(file);
            logger.warning("Eliminando archivo: " + file.getName() + " porque no cumple con el formato de nombre.");
            if (deleted) {
                logger.info("Archivo eliminado: " + file.getName());
            }
            return false;
        } else {
            logger.info("Archivo: " + file.getName() + " cumple con el formato de nombre.");
            return true;
        }
    }

    /**
     * Elimina un archivo de un bucket de Google Cloud Storage.
     *
     * @param file La instancia del archivo que se va a eliminar.
     * @return true si se eliminó correctamente, false si el archivo no existía
     */
    private boolean deleteFileFromBucket(Blob file) {
        try {
            // Instancia el cliente de GCS. En Cloud Run, la autenticación es automática.
            Storage storage = StorageOptions.getDefaultInstance().getService();

            // Crea el identificador único del archivo
            BlobId blobId = file.getBlobId();

            // Ejecuta la eliminación
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                System.out.println("Archivo " + file.getName() + " eliminado exitosamente del bucket " + file.getBucket());
            } else {
                System.out.println("El archivo " + file.getName() + " no se encontró en el bucket.");
            }

            return deleted;

        } catch (Exception e) {
            System.err.println("Error al intentar eliminar el archivo: " + e.getMessage());
            throw new RuntimeException("Error interactuando con Cloud Storage", e);
        }
    }
}