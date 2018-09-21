package edu.mcw.rgd.dataload;


import edu.mcw.rgd.datamodel.CytoBand;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author mtutaj
 * Date: 2/2/15
 */
public class CytomapLoader {
    private String version;
    private Map<String, String> ideoFiles;

    private ChromosomeDAO dao = new ChromosomeDAO();

    public void run(int mapKeyOverride) throws Exception {
        System.out.println(getVersion());
        //System.out.println("  MAP_KEY="+mapKeyOverride);

        for( Map.Entry<String,String> entry: ideoFiles.entrySet() ) {
            int mapKey = Integer.parseInt(entry.getKey());
            if( mapKeyOverride==mapKey ) {
                String fileName = entry.getValue();
                System.out.println("Downloading " + fileName);

                FileDownloader downloader = new FileDownloader();
                downloader.setExternalFile(fileName);
                downloader.setLocalFile("data/" + mapKey + ".txt.gz");
                downloader.setUseCompression(true);
                downloader.setPrependDateStamp(true);

                String localFile = downloader.downloadNew();
                parse(localFile, mapKey);
            }
        }
        System.out.println("  OK!");
    }

    void parse(String fileName, int mapKey) throws Exception {

        int bandRegions = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
        String line;
        while( (line=reader.readLine())!=null ) {
            String[] cols = line.split("[\\t]", -1);
            String chr = cols[0];
            // filter out chromosomes like this: chr10_AABR07050850v1_random
            if( chr.startsWith("chr") && !chr.contains("_"))
                chr = chr.substring(3);
            else
                continue;

            int startPos = 1+Integer.parseInt(cols[1]);
            int stopPos = 1+Integer.parseInt(cols[2]);
            String giemsaStain = cols[4];
            String bandName = cols[3];

            //System.out.println("chr"+chr+":"+startPos+".."+stopPos+" - "+bandName+" - "+bandStain);

            CytoBand cytoBand = dao.createCytoBand(mapKey, chr, bandName);
            cytoBand.setStartPos(startPos);
            cytoBand.setStopPos(stopPos);
            cytoBand.setGiemsaStain(giemsaStain);
            dao.updateCytoBand(cytoBand);
            bandRegions++;
        }
        reader.close();

        System.out.println("map_key="+mapKey+", "+fileName+", lines_loaded="+bandRegions);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setIdeoFiles(Map<String, String> ideoFiles) {
        this.ideoFiles = ideoFiles;
    }

    public Map<String, String> getIdeoFiles() {
        return ideoFiles;
    }
}
