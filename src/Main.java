import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.io.IOException;

/*
 * 1. C:/Users/User/Downloads/IPS_20240520 안에 압축 풀린 폴더 형태로 로그 폴더들이 들어가 있어야 함
 * 2. C:/Users/User/Downloads/filtered에 필터된 폴더 생성됨
 * */

public class Main {
    public static void main(String[] args) {
        String originPath = "C:/Users/User/Downloads/IPS_20240520";
        File originFolder = new File(originPath);

        // findLogFiles(originFolder, originPath);
        String findOutputPath = "C:/Users/User/Downloads/filtered";
        // mergeLogFiles(findOutputPath);
        String mergedPath = "C:/Users/User/Downloads/merged";
        // LogTimeCnt(mergedPath);
        String countPath = "C:/Users/User/Downloads/count";

        String countedPath = "C:/Users/User/Downloads/counted";
        // finalOutput(countedPath, "20"); // 날짜 20인 값 추출

    }

    private static void findLogFiles(File originFolder, String originPath) {
        File[] files = originFolder.listFiles();
        String parentPath = originFolder.getParent();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findLogFiles(file, originPath);
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {

                    filterLogFile(file, originFolder.getPath(), parentPath);
                }
            }
        } else {
            System.out.println("** 루트 경로 log 폴더에 log 파일 넣기 **");
        }
    }

    /*
     * 원본 로그에서 postaccept를 포함하고 있는 줄만 선별하는 작업
     * 각 폴더에 'filtered_파일명' 형식으로 저장
     * */
    private static void filterLogFile(File inputFile, String originPath, String parentPath) {

        File rootFolder = new File(parentPath);
        String rootPath = rootFolder.getParent();
        // System.out.println(parentPath); // ...\log\log
        // System.out.println(originPath); // ...\log\log\LI1
        // System.out.println(rootPath); // ...\log
        String outputPath = rootPath + "/filtered/"+"filtered_"+originPath.charAt(originPath.length()-1)+"/";
        File outputFolder = new File(outputPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        String outputFile = outputPath + "filtered_" + inputFile.getName();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("postaccept")) {
                    writer.println(line);
                }
            }
            System.out.println("saved to " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mergeLogFiles(String filteredRootPath) {
        File filteredFolder = new File(filteredRootPath);
        File[] subFolders = filteredFolder.listFiles(File::isDirectory);
        String mergedRootPath = "C:/Users/User/Downloads/merged";

        File mergedRootFolder = new File(mergedRootPath);
        if (!mergedRootFolder.exists()) {
            mergedRootFolder.mkdirs();
        }

        if (subFolders != null) {
            for (File subFolder : subFolders) {
                File[] logFiles = subFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
                if (logFiles != null) {
                    String mergedFileName = mergedRootPath + "/merged_" + subFolder.getName() + ".txt";
                    try (PrintWriter writer = new PrintWriter(new FileWriter(mergedFileName))) {
                        for (File logFile : logFiles) {
                            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    writer.println(line);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Merged log saved to " + mergedFileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("No subfolders found in the specified directory.");
        }
    }

    private static void LogTimeCnt(String mergedPath) {
        File folder = new File(mergedPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        String outputRootPath = "C:/Users/User/Downloads/counted";

        File outputRootFolder = new File(outputRootPath);
        if (!outputRootFolder.exists()) {
            outputRootFolder.mkdirs();
        }

        if (files != null) {
            for (File file : files) {
                Map<String, Map<String, Map<String, Map<String, Integer>>>> allCountMap = new TreeMap<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 타임스탬프 추출
                        String timestamp = line.substring(1, 15); // "03/17 23:18:08"
                        String date = timestamp.substring(0, 5); // "03/17"
                        String hour = timestamp.substring(6, 8); // "23"
                        String minute = timestamp.substring(9, 11); // "18"
                        String second = timestamp.substring(12, 14); // "08"

                        Map<String, Map<String, Map<String, Integer>>> hourMinuteSecondCountMap = allCountMap.getOrDefault(date, new TreeMap<>());
                        Map<String, Map<String, Integer>> minuteSecondCountMap = hourMinuteSecondCountMap.getOrDefault(hour, new TreeMap<>());
                        Map<String, Integer> secondCountMap = minuteSecondCountMap.getOrDefault(minute, new TreeMap<>());

                        // 해당 초의 개수를 증가시킴
                        secondCountMap.put(second, secondCountMap.getOrDefault(second, 0) + 1);

                        // 분-초 맵을 갱신함
                        minuteSecondCountMap.put(minute, secondCountMap);

                        // 시간-분-초 맵을 갱신함
                        hourMinuteSecondCountMap.put(hour, minuteSecondCountMap);

                        // 날짜-시간-분-초 맵을 갱신함
                        allCountMap.put(date, hourMinuteSecondCountMap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 결과를 파일에 작성
                String outputFilePath = outputRootPath + "/cnt_" + file.getName();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                    for (Map.Entry<String, Map<String, Map<String, Map<String, Integer>>>> dateEntry : allCountMap.entrySet()) {
                        String date = dateEntry.getKey();
                        Map<String, Map<String, Map<String, Integer>>> hourMinuteSecondCountMap = dateEntry.getValue();
                        for (Map.Entry<String, Map<String, Map<String, Integer>>> hourEntry : hourMinuteSecondCountMap.entrySet()) {
                            String hour = hourEntry.getKey();
                            Map<String, Map<String, Integer>> minuteSecondCountMap = hourEntry.getValue();
                            for (Map.Entry<String, Map<String, Integer>> minuteEntry : minuteSecondCountMap.entrySet()) {
                                String minute = minuteEntry.getKey();
                                Map<String, Integer> secondCountMap = minuteEntry.getValue();
                                for (Map.Entry<String, Integer> secondEntry : secondCountMap.entrySet()) {
                                    String second = secondEntry.getKey();
                                    int count = secondEntry.getValue();
                                    writer.write( date + "@" + hour + ":" + minute + ":" + second + "@" + count + "\n");
                                    // [05/19 23:59:50] lines: 18 -> 05/29@23:59:50@18
                                }
                            }
                        }
                    }
                    System.out.println("Result saved to " + outputFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No files found in the specified directory.");
        }
    }

    private static void finalOutput(String countedPath, String day) {
        File folder = new File(countedPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        String outputRootPath = "C:/Users/User/Downloads/final";

        File outputRootFolder = new File(outputRootPath);
        if (!outputRootFolder.exists()) {
            outputRootFolder.mkdirs();
        }

        if (files != null) {
            for (File file : files) {
                String outputFilePath = outputRootPath + "/final_" + file.getName();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 라인을 분해하여 날짜와 시간/카운트 부분을 추출
                            String[] parts = line.split("@");
                            String date = parts[0];
                            String time = parts[1];
                            String count = parts[2];

                            // 날짜가 day인 경우 시간과 카운트 부분만 출력
                            if (date.endsWith(day)) {
                                writer.write(time + "=" + count + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Filtered output saved to " + outputFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No files found in the specified directory.");
        }
    }
}