package com.kinplatform.billing.rips;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RipsZipService {

    private final RipsRecordRepository recordRepository;

    public byte[] createZip(RipsBatch batch) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batch.getId());

            Map<RipsBatch.RipsType, List<RipsRecord>> byType = records.stream()
                .collect(Collectors.groupingBy(RipsRecord::getRipsType));

            for (Map.Entry<RipsBatch.RipsType, List<RipsRecord>> entry : byType.entrySet()) {
                String filename = "rips_" + entry.getKey().name().toLowerCase() + ".xml";
                ZipEntry zipEntry = new ZipEntry(filename);
                zos.putNextEntry(zipEntry);

                String xml = buildRipsXml(entry.getValue());
                zos.write(xml.getBytes(StandardCharsets.UTF_8));

                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando ZIP para batch {}", batch.getId(), e);
            throw new RipsSerializationException("Error generando ZIP: " + e.getMessage(), e);
        }
    }

    private String buildRipsXml(List<RipsRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<RIPS>\n");

        for (RipsRecord record : records) {
            sb.append("  <REGISTRO>\n");
            sb.append("    <SECUENCIA>").append(record.getSequenceNumber()).append("</SECUENCIA>\n");
            sb.append("    <DATOS>").append(record.getRipsLineData()).append("</DATOS>\n");
            sb.append("  </REGISTRO>\n");
        }

        sb.append("</RIPS>");
        return sb.toString();
    }
}