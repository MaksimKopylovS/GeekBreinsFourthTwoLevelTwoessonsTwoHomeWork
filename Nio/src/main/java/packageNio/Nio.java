package packageNio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Set;

public class Nio {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private String rootPath = "server";


    public Nio() {
        try {
            ServerSocketChannel server = null;

            server = ServerSocketChannel.open();

            server.bind(new InetSocketAddress(8189));
            server.configureBlocking(false);
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started!");

            while (server.isOpen()) {
                selector.select();
                var selectionKeys = selector.selectedKeys();
                var iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    if (key.isAcceptable()) {
                        try {
                            handleAccept(key, selector);
                        } catch (NullPointerException nullPointerException) {
                            nullPointerException.printStackTrace();
                        }

                    }
                    if (key.isReadable()) {
                        try {
                            handleRead(key, selector);
                        } catch (NullPointerException nullPointerException) {
                            nullPointerException.printStackTrace();
                        }
                        handleRead(key, selector);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();
        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");

        String masCommand[] = command.split(" ");
        for (String comand : masCommand) {
            System.out.println(comand);
        }
        if (masCommand[0].equals("--help")) {
            channel.write(ByteBuffer.wrap("input ls for show file list".getBytes()));
        }

        if (masCommand[0].equals("ls")) {

            channel.write(ByteBuffer.wrap(getFilesList().getBytes()));
        }
//  touch (name) создать текстовый файл с именем
        if (masCommand[0].equals("touch")) {
            Path path = Paths.get("./" + rootPath + "/" + masCommand[1]);
            Files.createFile(path);

        }
//  mkdir (name) создать директорию
        if (masCommand[0].equals("mkdir")) {
            Path path = Paths.get("./" + rootPath + "/" + masCommand[1]);
            Path newDir = Files.createDirectory(path);
        }
//  cd (name) - перейти в папку
        if (masCommand[0].equals("cd")) {
            setRootPath(masCommand[1]);
        }
//  rm (name) удалить файл по имени
        if (masCommand[0].equals("rm")) {
            Files.delete(Paths.get("./" + rootPath + "/" + masCommand[1]));
            
        }
        //  copy (src, target) скопировать файл из одного пути в другой
        if (masCommand[0].equals("copy")) {
            Files.copy(Paths.get(masCommand[1]), Paths.get(masCommand[2]));

        }
        //  cat (name) - вывести в консоль содержимое файла
        if (masCommand[0].equals("cat")) {
            List<String> list = Files.readAllLines(Paths.get("./" + rootPath + "/" + masCommand[1]));
            for (String str: list) {
                channel.write(ByteBuffer.wrap(str.getBytes()));
                channel.write(ByteBuffer.wrap("\n".getBytes()));
            }

            }

    }

    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel) key.channel())
                        .write(ByteBuffer.wrap(message.getBytes()));
            }
        }
    }

    private String getFilesList() {

        return String.join(" ", new File(rootPath).list());
    }


    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "LOL");
        channel.write(ByteBuffer.wrap("Enter --help".getBytes()));
    }


    private void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public static void main(String[] args) {

        new Nio();

    }
}
