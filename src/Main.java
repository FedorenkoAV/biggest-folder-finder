import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        String folderPath = "d:\\Книги\\";
        File file = new File(folderPath);
        long start = System.currentTimeMillis();

        FolderSizeCalculator calculator =
                new FolderSizeCalculator(file);
        ForkJoinPool pool = new ForkJoinPool();
        long size = pool.invoke(calculator);
        System.out.println(size);

        //System.out.println(getFolderSize(file));

        long duration = System.currentTimeMillis() - start;
        System.out.println(duration + " ms");
    }

    public static long getFolderSize(File folder) {
        if (folder.isFile()) {
            return folder.length();
        }
        long sum = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                sum += getFolderSize(file);
            }
        }
        return sum;
    }

    //Вылетает с ошибкой при просмотре папки c:\$Recycle.Bin\ в Windows
    // https://bugs.openjdk.java.net/browse/JDK-8039910
    private static long getFolderSize2(File folder) {
        Path path = folder.toPath();
        long sizeInBytes = -1L;
        try (Stream<Path> walk = Files.walk(path)) {
            sizeInBytes = walk
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .mapToLong(File::length)
                    .sum();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sizeInBytes;
    }

    private static long getFolderSize3(File folder) {
        Path path = folder.toPath();
        final long[] sizeInBytes = {0L};
        try {
            Files.walkFileTree(path, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    sizeInBytes[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sizeInBytes[0];
    }

    private static String getHumanReadable(long size) {
        int index = -1;
        char[] chArray = {'K', 'M', 'G', 'T', 'P', 'E'};
        if (size < 1024) {
            return size + " B";
        }
        double doubleNumber = size;
        while (doubleNumber >= 1024.0) {
            doubleNumber /= 1024.0;
            index++;
        }
        return String.format("%.1f%s", doubleNumber, " " + chArray[index] + "b");
    }


}
