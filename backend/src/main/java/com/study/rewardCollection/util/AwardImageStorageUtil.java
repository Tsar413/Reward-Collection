package com.study.rewardCollection.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public final class AwardImageStorageUtil {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private AwardImageStorageUtil() {
    }

    public record StoredImage(
            String storageKey,
            String originalFilename,
            String mimeType,
            long fileSize,
            int width,
            int height
    ) {
    }

    private record ImageInfo(
            String extension,
            String mimeType,
            int width,
            int height
    ) {
    }

    public static StoredImage save(
            Path root,
            Long orgId,
            byte[] bytes,
            String originalFilename
    ) throws IOException {
        if (orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("组织ID无效");
        }

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("图片不能为空");
        }

        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("图片不能超过5MB");
        }

        ImageInfo info = inspect(bytes);
        LocalDate date = LocalDate.now();

        String key = orgId + "/"
                + date.getYear() + "/"
                + String.format(
                Locale.ROOT, "%02d", date.getMonthValue()
        )
                + "/" + UUID.randomUUID()
                + "." + info.extension();

        Path target = resolve(root, key);
        Files.createDirectories(target.getParent());

        boolean created = false;

        try {
            try (OutputStream out = Files.newOutputStream(
                    target,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            )) {
                created = true;
                out.write(bytes);
            }
        } catch (IOException e) {
            if (created) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanup) {
                    e.addSuppressed(cleanup);
                }
            }
            throw e;
        }

        return new StoredImage(
                key,
                cleanName(originalFilename, info.extension()),
                info.mimeType(),
                bytes.length,
                info.width(),
                info.height()
        );
    }

    private static ImageInfo inspect(byte[] bytes) {
        try (MemoryCacheImageInputStream input =
                     new MemoryCacheImageInputStream(
                             new ByteArrayInputStream(bytes)
                     )) {

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException(
                        "文件不是支持的图片"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);

                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                String extension;
                String mimeType;

                switch (format) {
                    case "jpeg", "jpg" -> {
                        extension = "jpg";
                        mimeType = "image/jpeg";
                    }
                    case "png" -> {
                        extension = "png";
                        mimeType = "image/png";
                    }
                    case "webp" -> {
                        extension = "webp";
                        mimeType = "image/webp";
                    }
                    default -> throw new IllegalArgumentException(
                            "只支持JPEG、PNG、WebP图片"
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0
                        || height <= 0
                        || width > 10000
                        || height > 10000
                        || (long) width * height > 25_000_000) {
                    throw new IllegalArgumentException(
                            "图片尺寸过大，最多2500万像素且单边不超过10000"
                    );
                }

                // 实际解码，不能只检查扩展名和文件头。
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException(
                            "图片无法解码"
                    );
                }

                return new ImageInfo(
                        extension, mimeType, width, height
                );
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "图片损坏或无法解码", e
            );
        }
    }

    public static byte[] read(
            Path root,
            String key
    ) throws IOException {
        try (InputStream input =
                     Files.newInputStream(resolve(root, key))) {

            byte[] bytes = input.readNBytes(MAX_BYTES + 1);

            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw new IOException("已存储图片大小异常");
            }

            return bytes;
        }
    }

    public static void delete(
            Path root,
            String key
    ) throws IOException {
        Files.deleteIfExists(resolve(root, key));
    }

    private static Path resolve(Path root, String key) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path target = normalizedRoot.resolve(key).normalize();

        if (!target.startsWith(normalizedRoot)
                || target.equals(normalizedRoot)) {
            throw new IllegalArgumentException(
                    "图片存储路径无效"
            );
        }

        return target;
    }

    private static String cleanName(
            String filename,
            String extension
    ) {
        String name = filename == null
                ? "" : filename.replace('\\', '/');

        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();

        if (name.isBlank()) {
            name = "image." + extension;
        }

        if (name.codePointCount(0, name.length()) > 255) {
            name = name.substring(
                    0, name.offsetByCodePoints(0, 255)
            );
        }

        return name;
    }
}