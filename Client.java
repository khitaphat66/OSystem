
import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private SocketChannel socketChannel;
    private final String foldercopy = "D:/OS/Client/Copy/";  //<<<<<<<<<<<<<<<<<<<<<<<<
    private final String folderzero = "D:/OS/Client/Zero/";  //<<<<<<<<<<<<<<<<<<<<<<<<
    private final String IpAddress = "192.168.43.152"; //192.168.56.1
    private final int port = 5152;
    private final int portChannel = 5157;
    private String[] fileList;

    public Client() {
        connection();
        if (fromServer != null) {
            getFileList();          //เรียกเมธอดเพื่อรับรายชื่อไฟล์อยู่ใน server
            requestServer();  //เรียกเมธอดนี้เพื่อเริ่มกระบวนการรับค่าและดาวน์โหลดไฟล์
        } else {
            System.out.println("Failed to establish connection with the server.");
        }
    }

    public final void connection() {
        try {
            socket = new Socket(IpAddress, port);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(IpAddress, portChannel));
            if (socket.isConnected()) {     //ตรวจสอบว่ามีการเชื่อมต่อสำเร็จหรือไม่
                System.out.println("Connected to the server.");
            } else {
                System.out.println("Connection to the server failed.");
            }
        } catch (IOException e) {
            System.out.println("Connection to the server failed: " + e.getMessage());
        }
    }

    public final void getFileList() {
        try {
            String read, file = "";  //ตัวแปร read อ่านข้อมูลจาก fromServer, file เป็น String ว่าง ๆ 
            while (!(read = fromServer.readUTF()).equalsIgnoreCase("/EOF")) {
                file += read + "/";      //อัพเดทค่า file โดยการเพิ่มค่าของ read และเครื่องหมาย /
            }
            fileList = file.split("/");     //วนลูปเสร็จ รายชื่อไฟล์ที่รับมาจะแบ่งออกและเก็บใน fileList 
            printFile();
        } catch (IOException e) {
            disconnect();
        }
    }

    public final void printFile() {
        for (int i = 0; i < fileList.length; i++) {
            System.out.println(" [" + (i + 1) + "] " + fileList[i]);
        }
    }

    public final void requestServer() {

        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.println("Wolud you like to download file? (Yes/No)");
            String request = scan.next();
            if (request.equalsIgnoreCase("No") || request.equalsIgnoreCase("N")) {
                break;
            } else if (request.equalsIgnoreCase("Yes") || request.equalsIgnoreCase("Y")) {
                System.out.print("which file do you want to download => ");
                String number = scan.next();
                try {
                    int index = Integer.parseInt(number) - 1;
                    if (index < 0 || index >= fileList.length) {
                        System.out.println("Invalid file no.\n");
                        continue;
                    }
                    System.out.println("Choose one type for send\n1.Copy\n2.zero copy");
                    System.out.print("Select type -->");
                    String type = scan.next();
                    if (!type.equals("1") && !type.equals("2")) {
                        System.out.println("Invalid type\n");
                        printFile();
                        continue;
                    }
                    toServer.writeInt(index);
                    toServer.writeUTF(type);
                    long size = fromServer.readLong();
                    long start = System.currentTimeMillis();
                    String filePathcopy = foldercopy + fileList[index];
                    String filePathzero = folderzero + fileList[index];
                    if (type.equals("1")) {
                        copy(filePathcopy, size, index);
                    } else if (type.equals("2")) {
                        zeroCopy(filePathzero, size, index);
                    }
                    long end = System.currentTimeMillis();
                    long timeElaspe = end - start;
                    System.out.println("Time Elaspe: " + timeElaspe + " ms\n");
                    toServer.writeLong(timeElaspe);
                    printFile();
                } catch (NumberFormatException e) {
                    System.out.println("** No information ****\n");
                } catch (IOException ex) {
                }
            }
        }
    }

    public void copy(String filePath, long size, int index) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int read;
            long currentRead = 0;
            while (currentRead < size && (read = fromServer.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                currentRead += read;
            }
            System.out.println("Copy Success");
        } catch (IOException e) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                disconnect();
            }
        }
    }

    public final void zeroCopy(String filePath, long size, int index) {
        FileChannel destination = null;
        try {
            destination = new FileOutputStream(filePath).getChannel();
            long currentRead = 0;
            long read;
            while (currentRead < size && (read = destination.transferFrom(socketChannel, currentRead, size - currentRead)) != -1) {
                currentRead += read;
            }
            System.out.println("ZeroCopy Success");
        } catch (IOException e) {
        } finally {
            try {
                if (destination != null) {
                    destination.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public void disconnect() {    //ใช้สำหรับปิดการเชื่อมต่อ
        try {
            if (fromServer != null) {
                fromServer.close();
            }
            if (toServer != null) {
                toServer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
    }
}
