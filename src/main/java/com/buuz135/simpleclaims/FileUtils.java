package com.buuz135.simpleclaims;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.json.JsonWriter;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

import static com.hypixel.hytale.server.core.util.BsonUtil.BSON_DOCUMENT_CODEC;
import static com.hypixel.hytale.server.core.util.BsonUtil.SETTINGS;

public class FileUtils {

    public static String MAIN_PATH = "." + File.separator + "SimpleClaims";
    public static String PARTY_PATH = MAIN_PATH + File.separator + "Parties.json";
    public static String CLAIM_PATH = MAIN_PATH + File.separator + "Claims.json";
    public static String NAMES_CACHE_PATH = MAIN_PATH + File.separator + "NameCache.json";

    public static void ensureDirectory(String path){
        var file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void ensureMainDirectory(){
        ensureDirectory(MAIN_PATH);
    }

    public static File ensureFile(String path, String defaultContent){
        var file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                var writer = new FileWriter(file);
                writer.write(defaultContent);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static <T> void writeSync(@Nonnull Path path, @Nonnull Codec<T> codec, T value, @Nonnull HytaleLogger logger) throws IOException {
        Path parent = PathUtil.getParent(path);
        if (!Files.exists(parent, new LinkOption[0])) {
            Files.createDirectories(parent);
        }

        if (Files.isRegularFile(path, new LinkOption[0])) {
            Path resolve = path.resolveSibling(String.valueOf(path.getFileName()) + ".bak");
            Files.move(path, resolve, StandardCopyOption.REPLACE_EXISTING);
        }

        ExtraInfo extraInfo = (ExtraInfo)ExtraInfo.THREAD_LOCAL.get();
        BsonValue bsonValue = codec.encode(value, extraInfo);
        extraInfo.getValidationResults().logOrThrowValidatorExceptions(logger);


        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            var jsonWriter = new JsonWriter(writer);

        }

    }
}
