package imageValidator;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.tika.Tika;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class SecureFileAnalyzer {

    public static final Logger logger = Logger.getLogger(SecureFileAnalyzer.class.getName());

    /**
     * Detecta el tipo MIME real de un archivo en GCS leyendo sus "Magic Numbers".
     *
     * @param bucketName El nombre de tu bucket
     * @param objectName La ruta y nombre del archivo
     * @return El tipo de archivo real (ej. "application/pdf")
     */
    public static String getRealContentType(String bucketName, String objectName) {
        // Instancia de Tika para análisis de contenido
        Tika tika = new Tika();

        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);

        // Usamos 8KB (8192 bytes), lo cual es más que suficiente
        // para que Tika detecte la firma de casi cualquier formato.
        ByteBuffer buffer = ByteBuffer.allocate(8192);

        try (ReadChannel reader = storage.reader(blobId)) {
            // Lee solo el primer fragmento del archivo directamente del bucket
            reader.read(buffer);

            // Prepara el buffer para ser leído por Tika
            buffer.flip();

            // Extrae el arreglo de bytes real
            byte[] fileHeader = new byte[buffer.limit()];
            buffer.get(fileHeader);

            // Tika analiza los bytes crudos y devuelve el tipo MIME real
            String realMimeType = tika.detect(fileHeader);
            logger.info("Tipo MIME real detectado: " + realMimeType);
            return realMimeType;

        } catch (Exception e) {
            logger.severe("Error al procesar el evento: " + e.getMessage());
            throw new RuntimeException("No se pudo verificar el tipo de archivo", e);
        }
    }
}