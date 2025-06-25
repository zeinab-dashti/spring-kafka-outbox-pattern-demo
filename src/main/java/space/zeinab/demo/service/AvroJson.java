package space.zeinab.demo.service;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class AvroJson {
    public static <T extends SpecificRecord> String toJson(T record) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), out);
            DatumWriter<T> writer = new SpecificDatumWriter<>(record.getSchema());
            writer.write(record, encoder);
            encoder.flush();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

