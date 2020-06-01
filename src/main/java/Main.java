import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.metainfo.MetadataService;
import bt.metainfo.Torrent;
import bt.runtime.BtClient;
import com.google.inject.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("BitTorrentAPP");

        File localPath = new File(System.getProperty("user.dir"));
        File newDirectory = new File(localPath, "Download");

        if (!newDirectory.exists())
            if (newDirectory.mkdir())
             System.out.println("Directory creation succeeded");

        // create file system based backend for torrent data
        Storage storage = new FileSystemStorage(newDirectory.toPath());

        Scanner in = new Scanner(System.in);
        String torrentFilePath;
        File torrentFile;

        do {
            System.out.print("Input a name of torrent file: ");
            torrentFilePath = in.nextLine();
            torrentFile = new File(torrentFilePath);
        }
        while (!torrentFile.exists());

        System.out.println("File '" + torrentFilePath + "' exists!");

        MetadataService metadataService = new MetadataService();
        Torrent torrent = metadataService.fromInputStream(new FileInputStream(torrentFile));

        System.out.println("Name: "+torrent.getName());
        System.out.println("Creation date: "+torrent.getCreationDate().get());
        System.out.println("Size: "+torrent.getSize());
        System.out.println("ChunkSize: "+torrent.getChunkSize());
        System.out.println("NumberOfFiles: " + torrent.getFiles().size());

        System.out.println("TrackerUrls:");
        for (List<String> urls : torrent.getAnnounceKey().get().getTrackerUrls()) {
            for (String url : urls) {
                System.out.println("\t"+url);
            }
        }


        Module dhtModule = new DHTModule(new DHTConfig() {
            @Override
            public boolean shouldUseRouterBootstrap() {
                return true;
            }
        });

        BtClient client = Bt.client()
                .storage(storage)
                .torrent(torrentFile.toURI().toURL())
                .autoLoadModules()
                .module(dhtModule)
                .stopWhenDownloaded()
                .build();

        LocalDateTime start = LocalDateTime.now();
        System.out.println("Start of downloading: " + start);

        client.startAsync(state -> {
            if (state.getPiecesRemaining() == 0) {
                LocalDateTime end = LocalDateTime.now();
                System.out.println("End of downloading " + end);
                Duration duration = Duration.between(start, end);
                System.out.println("Elapsed time: " + duration.getSeconds() + " seconds");
                System.exit(0);

            }
        }, 1000).join();

        //client.stop();



    }
}
