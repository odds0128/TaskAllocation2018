import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut extends ShowGraph implements SetParam {
    static FileWriter fw;
    static BufferedWriter bw;
    static PrintWriter pw;
    static private OutPut singleton = new OutPut();
    static int zeroAgents = 0;
    static int[] means = new int[100];
    static int n;
    static int i = 1;
    static String outputFilePath = "src/out.xlsx";
    static Workbook book = null;
    static FileOutputStream fout = null;


/*
    public OutPut getInstance(){
        return singleton;
    }
// */

    OutPut() {
        try {
            fw = new FileWriter("/Users/r.funato/IdeaProjects/TaskAllocation/src/output" + i + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            pw.println("turn" + ", " + " " + "FinishedTasks" + "," + "Differences" + ", " + "Messages" + ", " + "Reciprocal" + ", " + "Rational" + ", " + "Leader" + ", " + "Member");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     *
     * @param taskQueue
     */
    static void checkTask(Queue<Task> taskQueue) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.poll();
            System.out.println(temp);
            System.out.println(temp.subTasks);
            taskQueue.add(temp);
        }
        System.out.println("  Remains: " + taskQueue.size());
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[] agents) {
        int tempL = 0, tempM = 0;
        System.out.println("Total Agents is " + Agent._id);
        for (Agent ag : agents) {
            if (ag.e_leader > ag.e_member) tempL++;
            else tempM++;
        }
        System.out.println("Leaders is " + tempL + ", Members is " + tempM);
        List<Agent> list = Arrays.asList(agents);
        Collections.sort(list, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(agents[i]);
        }
    }

    /**
     * checkAgentメソッド
     * 二次元配列の場合
     */
    static void checkGrids(Agent[][] grids) {
        System.out.println("Total Agents is " + Agent._id);
        System.out.println("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num);
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                if (grids[i][j] == null) System.out.print(" 　 ");
                else System.out.print(String.format("%3d ", grids[i][j].id));
            }
            System.out.println();
        }
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);
        System.out.println("Total Agents is " + Agent._id);
        System.out.println("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num + ", Resources : ");

/*        for( int i = 0; i < RESOURCE_NUM; i++ ) System.out.print( Agent.resSizeArray[i] + ", " );
        System.out.println();
        // */
/*
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print("ID : " + i + ", ");
            for( int j = 0; j < AGENT_NUM; j++ ){
                System.out.print(String.format("%3d", Manager.distance[i][j]));
            }
            System.out.println();
        }
// */
        Collections.sort(temp, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(temp.get(i));
        }
// */
    }

    // ある時点でのパラメータを表示するメソッド
    static void showResults(int turn, List<Agent> agents) {
        int tempL = 0, tempM = 0;

        zeroAgents = countZeroAgent(agents);
        System.out.println(" Turn: " + turn);
        System.out.println("  Total Agents is " + Agent._id);
        int temp = countReciplocalist(agents);
        for (Agent ag : agents) {
            if (ag.e_leader > ag.e_member) tempL++;
            else tempM++;
        }
        System.out.println("Leaders is " + tempL + ", Members is " + tempM);
        System.out.println("  Leaders is " + tempL + ", Members is " + tempM
                + " (Rationalists: " + (AGENT_NUM - temp) + ", Reciprocalists: " + (temp) + "), "
                + " (Reciplocal member: " + countReciplocalMembers(agents) + " )"
                + "ZeroAgents: " + zeroAgents);
        System.out.println("  Messages  : " + TransmissionPath.messageNum
                + ", Mean TransmitTime: " + String.format("%.2f", (double) TransmissionPath.transmitTime / (double) TransmissionPath.messageNum)
                + ", Proposals: " + TransmissionPath.proposals + ", Replies: " + TransmissionPath.replies
                + ", Acceptances: " + TransmissionPath.acceptances + ", Rejects: " + TransmissionPath.rejects);
        System.out.println("   , Results: " + TransmissionPath.results
                + ", Finished: " + TransmissionPath.finished + ", Failed:" + TransmissionPath.failed);
        System.out.println("  Finished tasks: " + Manager.finishedTasks + ", Disposed tasks: " + Manager.disposedTasks
                + ", Overflow tasks: " + Manager.overflowTasks + ", Processing tasks: " + Manager.processingTasks
                + ", Resting tasks: " + Manager.taskQueue.size() + ", RedoSubTasks: " + Manager.redoSubtasks);
// */
    }

    static void showExecutionTimeTable(Strategy strategy, List<Agent> agents) {
        if (strategy.getClass().getName() != "ProposedMethod2") return;
        else {
            for (int i = 0; i < AGENT_NUM; i++) {
            }
        }

    }

    static void writeExcels(List<Agent> agents) throws FileNotFoundException, IOException {
        Edge.makeEdge(agents);
        assert Edge.to_id.size() == Edge.from_id.size() : "oioi";
        try {

            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("ＭＳ ゴシック");
            font.setFontHeightInPoints((short) 9);

            DataFormat format = book.createDataFormat();

            //ヘッダ文字列用のスタイル
            CellStyle style_header = book.createCellStyle();
            style_header.setBorderBottom(BorderStyle.THIN);
            OutPut.setBorder(style_header, BorderStyle.THIN);
            style_header.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex());
            style_header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style_header.setVerticalAlignment(VerticalAlignment.TOP);
            style_header.setFont(font);

            //文字列用のスタイル
            CellStyle style_string = book.createCellStyle();
            OutPut.setBorder(style_string, BorderStyle.THIN);
            style_string.setVerticalAlignment(VerticalAlignment.TOP);
            style_string.setFont(font);

            //改行が入った文字列用のスタイル
            CellStyle style_string_wrap = book.createCellStyle();
            OutPut.setBorder(style_string_wrap, BorderStyle.THIN);
            style_string_wrap.setVerticalAlignment(VerticalAlignment.TOP);
            style_string_wrap.setWrapText(true);
            style_string_wrap.setFont(font);

            //整数用のスタイル
            CellStyle style_int = book.createCellStyle();
            OutPut.setBorder(style_int, BorderStyle.THIN);
            style_int.setDataFormat(format.getFormat("###0;-###0"));
            style_int.setVerticalAlignment(VerticalAlignment.TOP);
            style_int.setFont(font);

            //小数用のスタイル
            CellStyle style_double = book.createCellStyle();
            OutPut.setBorder(style_double, BorderStyle.THIN);
            style_double.setDataFormat(format.getFormat("###0.0;-###0.0"));
            style_double.setVerticalAlignment(VerticalAlignment.TOP);
            style_double.setFont(font);

            //円表示用のスタイル
            CellStyle style_yen = book.createCellStyle();
            OutPut.setBorder(style_yen, BorderStyle.THIN);
            style_yen.setDataFormat(format.getFormat("\"\\\"###0;\"\\\"-###0"));
            style_yen.setVerticalAlignment(VerticalAlignment.TOP);
            style_yen.setFont(font);

            //パーセント表示用のスタイル
            CellStyle style_percent = book.createCellStyle();
            OutPut.setBorder(style_percent, BorderStyle.THIN);
            style_percent.setDataFormat(format.getFormat("0.0%"));
            style_percent.setVerticalAlignment(VerticalAlignment.TOP);
            style_percent.setFont(font);

            //日時表示用のスタイル
            CellStyle style_datetime = book.createCellStyle();
            OutPut.setBorder(style_datetime, BorderStyle.THIN);
            style_datetime.setDataFormat(format.getFormat("yyyy/mm/dd hh:mm:ss"));
            style_datetime.setVerticalAlignment(VerticalAlignment.TOP);
            style_datetime.setFont(font);

            Row row;
            int rowNumber;
            Cell cell;
            int colNumber;


            Sheet sheet;

            for (int i = 0; i < 3; i++) {
                sheet = book.createSheet();
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                //シート名称の設定
                if (i == 0)       book.setSheetName(i, "nodes");
                else if( i == 1 ) book.setSheetName(i, "edges");
                else book.setSheetName(i, "reciprocalEdges");

                //ヘッダ行の作成
                rowNumber = 0;
                colNumber = 0;

                row = sheet.createRow(rowNumber);
                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node id");
                else if( i == 1 ) cell.setCellValue("Edge id");
                else cell.setCellValue("Edge id");

                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node color");
                else if( i == 1 ) cell.setCellValue("Source Node id");
                else cell.setCellValue("Source Node id");

                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node shape");
                else if( i == 1 ) cell.setCellValue("Target Node id");
                else cell.setCellValue("Target Node id");

                if( i == 0 ) {
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" x-coordinate ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" y-coordinate ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" leader id");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" distance to leader ");
                }

                //ウィンドウ枠の固定
                sheet.createFreezePane(1, 1);

                //ヘッダ行にオートフィルタの設定
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colNumber));

                //列幅の自動調整
                for (int j = 0; j <= colNumber; j++) {
                    sheet.autoSizeColumn(j, true);
                }

                //nodeシートへの書き込み
                if (i == 0) {
                    int j = 0;
                    for (Agent agent : agents) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.id);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.STRING);
                        if( agent.e_leader > agent.e_member)     cell.setCellValue("Red");
                        else if( agent.principle == RATIONAL )   cell.setCellValue("Green");
                        else if( agent.principle == RECIPROCAL ) cell.setCellValue("Blue");

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.STRING);
                        if( agent.e_leader > agent.e_member)     cell.setCellValue("Circle");
                        else if( agent.principle == RATIONAL )   cell.setCellValue("Square");
                        else if( agent.principle == RECIPROCAL ) cell.setCellValue("Triangle");

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.x * 10);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.y * 10);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if( agent.e_leader > agent.e_member ) cell.setCellValue(agent.id * 3);
                        else if( agent.leader != null ) cell.setCellValue(agent.leader.id * 3);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if( agent.e_leader > agent.e_member ) cell.setCellValue(0);
                        else if( agent.leader != null ) cell.setCellValue(Manager.distance[agent.id][agent.leader.id]*10);

                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
                // edgeシートへの書き込み
                else if( i == 1 ){
                    for (int j = 0; j < Edge.from_id.size(); j++) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(j);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(Edge.from_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(Edge.to_id.get(j));

                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }else{
                    for (int j = 0; j < Edge.from_id.size(); j++) {
                        if( Edge.isRecipro.get(j) != true ) continue;
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(j);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(Edge.from_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(Edge.to_id.get(j));

                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
            }
            //ファイル出力
            fout = new FileOutputStream(outputFilePath);
            book.write(fout);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
            if (book != null) {
                try {
                    /*
                        SXSSFWorkbookはメモリ空間を節約する代わりにテンポラリファイルを大量に生成するため、
                        不要になった段階でdisposeしてテンポラリファイルを削除する必要がある
                     */
                    ((SXSSFWorkbook) book).dispose();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * writeResultsメソッド
     * ファイルへの結果書き込み
     *
     * @param turn
     * @param agents
     */
    static void writeResults(int turn, List<Agent> agents) {
        for (int i = 1; i <= WRITE_NUM; i++) {
            pw.print(i * (TURN_NUM / WRITE_NUM) + ", " + Manager.meanFinishedTasksArray[i - 1] / EXECUTE_NUM + ", ");
            pw.print((Manager.meanFinishedTasksArray[i] - Manager.meanFinishedTasksArray[i - 1]) / EXECUTE_NUM + ", ");
            pw.print(Manager.meanMessagesArray[i - 1] / EXECUTE_NUM);
/*            pw.print(Agent._leader_num + ", " + Agent._member_num + ", ");

// */
            pw.println();
        }
        pw.println(Manager.meanReciprocal / EXECUTE_NUM + ", " + Manager.meanRational / EXECUTE_NUM);
        pw.println(Manager.meanLeader / EXECUTE_NUM + ", " + Manager.meanMember / EXECUTE_NUM);
    }

    static void writeReliabilities(List<Agent> agents) {
        for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
        pw.println();
        for (Agent ag : agents) {
            if (ag.id != 3) continue;
            pw.print(ag.id + ", ");
            for (int i = 0; i < AGENT_NUM; i++) pw.print(ag.reliabilities[i] + ", ");
            pw.println();
        }
        pw.println();
    }

    /**
     * メソッド
     * 過去にどのエージェントとどのエージェントが何回組んだかと, そのエージェントの役割を書き込む
     * メンバは負, リーダーは正
     */
    static void writeCoalitions(List<Agent> agents) {
        for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
        pw.println();
        for (Agent ag : agents) {
            pw.print(ag.id + ", ");
            if (ag.e_leader > ag.e_member) for (int i = 0; i < AGENT_NUM; i++) pw.print( (-1) * ag.workWithAsL[i] + ", ");
            else for (int i = 0; i < AGENT_NUM; i++) pw.print(ag.workWithAsM[i] + ", ");
            pw.println();
        }
        pw.println();
    }

    // 座標上でどういう風に信頼関係ができているかをみるメソッド
    static void showDistributions(Agent[][] grids) {
        for (int i = 0; i < ROW / 2; i++) {
            for (int j = 0; j < COLUMN / 2; j++) {
                if (grids[i][j] != null) {
                    if (grids[i][j].role == LEADER) System.out.print(String.format("l%3d", grids[i][j].id));
                    else System.out.print(String.format("%4d", grids[i][j].relAgents.get(0).id));
                } else System.out.print("    ");
            }
            System.out.println();
        }
    }

    static void calcMeaning() {
        n = 0;
        means[n++] += Agent._leader_num;
        means[n++] += Agent._member_num;
        means[n++] += TransmissionPath.messageNum;
        means[n++] += zeroAgents;
    }

    static void showMeanings() {
        for (int i = 0; i < n; i++) {
            means[i] /= EXECUTE_NUM;
        }
        System.out.println("Averages: ");
        System.out.print("Leaders: " + means[0] + ", Members: " + means[1] + ", Messages: " + means[2] + ", ZeroAgents: " + means[3]);
        System.out.println();
        pw.println();
        pw.println("Averages" + "," + "Leaders" + ", " + "Members" + ", " + "Messages" + ", " + "ZeroAgents");
        pw.print("," + means[0] + ", " + means[1] + ", " + means[2] + ", " + means[3]);
    }

    void showGraph(List<Agent> agents) {
        super.show2DGraph(agents);
    }

    // 改行するだけのメソッド
    public static void newLine() {
        pw.println();
    }

    // こなしたタスク数によって度数分布を表示
    static void showFrequency(List<Agent> agents) {
        int partition = 100;
        int range = 10;
        int max = partition * range;
        int zeroAgent = 0;
        int frequenciesMembers[] = new int[partition + 1];
        int frequenciesLeaders[] = new int[partition + 1];
        for (Agent agent : agents) {
            if (agent.didTasksAsMember == 0 && agent.didTasksAsLeader == 0) {
                zeroAgent++;
                continue;
            }
            if (agent.didTasksAsMember > max) frequenciesMembers[partition]++;
            else frequenciesMembers[agent.didTasksAsMember / range]++;
            if (agent.didTasksAsLeader > max) frequenciesLeaders[partition]++;
            else frequenciesLeaders[agent.didTasksAsLeader / range]++;
        }
        System.out.println(" ZeroAgents: " + zeroAgent);

        int j = 0;
        System.out.println("AsMembers: ");
        for (int frequency : frequenciesMembers) {
            if (frequency == 0) continue;
            System.out.print(String.format("%4d", j) + " ~ " + String.format("%4d", j += 10) + ": " + String.format("%4d", frequency) + ": ");
            for (int i = 0; i < frequency / 10; i++) System.out.print("*");
            System.out.println();
        }
        j = 0;
        System.out.println("AsLeaders: ");
        for (int frequency : frequenciesLeaders) {
            if (frequency == 0) continue;
            System.out.print(String.format("%4d", j) + " ~ " + String.format("%4d", j += 10) + ": " + String.format("%4d", frequency) + ": ");
            for (int i = 0; i < frequency / 10; i++) System.out.print("*");
            System.out.println();
        }
    }

    static int countZeroAgent(List<Agent> agents) {
        int temp = 0;
        for (Agent agent : agents) {
            if ((agent.didTasksAsMember + agent.didTasksAsLeader) == 0) temp++;
        }
        return temp;
    }

    static int countReciplocalist(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
            if (agent.reliabilities[agent.relRanking.get(0).id] > THRESHOLD_DEPENDABILITY && (agent.e_member > THRESHOLD_RECIPROCITY || agent.e_leader > THRESHOLD_RECIPROCITY))
                temp++;
        }
        return temp;
    }

    static int countReciplocalMembers(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
            if (agent.reliabilities[agent.relRanking.get(0).id] > THRESHOLD_DEPENDABILITY && agent.e_member > THRESHOLD_RECIPROCITY && agent.e_member > agent.e_leader )
                temp++;
        }
        return temp;
    }


    static void fileClose() {
        pw.close();
    }

    private static void setBorder(CellStyle style, BorderStyle border) {
        style.setBorderBottom(border);
        style.setBorderTop(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
    }

    private final static String[] LIST_ALPHA = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    private static String getExcelColumnString(int column) {
        String result = "";

        if (column >= 0) {
            if (column / OutPut.LIST_ALPHA.length > 0) {
                result += getExcelColumnString(column / OutPut.LIST_ALPHA.length - 1);
            }
            result += OutPut.LIST_ALPHA[column % OutPut.LIST_ALPHA.length];
        }

        return result;
    }
}
