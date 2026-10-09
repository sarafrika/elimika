package apps.sarafrika.elimika.shared.storage.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

/**
 * Downscales raster images to a target width, honouring EXIF orientation.
 * Opaque results are JPEG; images with transparency stay PNG.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ImageVariantGenerator {

    /** Refuse to decode anything larger than this, to bound heap use per request. */
    static final long MAX_SOURCE_PIXELS = 24_000_000L;
    private static final float JPEG_QUALITY = 0.82f;

    record Variant(byte[] bytes, String extension) {
    }

    /**
     * Returns the resized variant, or empty when the source is already no wider
     * than {@code width}, is unreadable, or exceeds {@link #MAX_SOURCE_PIXELS}.
     */
    static Optional<Variant> resize(byte[] source, int width) throws IOException {
        int[] dimensions = readDimensions(source);
        if (dimensions == null
                || (long) dimensions[0] * dimensions[1] > MAX_SOURCE_PIXELS
                || dimensions[0] <= width) {
            return Optional.empty();
        }
        BufferedImage scaled = Thumbnails.of(new ByteArrayInputStream(source))
                .width(width)
                .asBufferedImage();
        if (scaled.getColorModel().hasAlpha()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", out);
            return Optional.of(new Variant(out.toByteArray(), "png"));
        }
        return Optional.of(new Variant(writeJpeg(scaled), "jpg"));
    }

    private static int[] readDimensions(byte[] source) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
