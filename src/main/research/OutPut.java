package main.research;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import main.research.agent.Agent;
import main.research.communication.TransmissionPath;
import main.research.graph.Edge;
import main.research.task.Task;

import static main.research.SetParam.Role.*;
import static main.research.SetParam.Principle.*;

import java.text.SimpleDateFormat;

import java.io.*;
import java.util.*;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut implements SetParam {
    static Workbook book = null;
    static FileOutputStream fout = null;
    static OutPut _singleton = new OutPut();

    static int index = 0;

    static int[] finishedTasksArray = new int[WRITING_TIMES];
    static int[] disposedTasksArray = new int[WRITING_TIMES];
    static int[] overflownTasksArray = new int[WRITING_TIMES];
    static int[] messagesArray = new int[WRITING_TIMES];
    static double[] communicationDelayArray = new double[WRITING_TIMES];
    static int[] leaderNumArray = new int[WRITING_TIMES];
    static int[] memberNumArray = new int[WRITING_TIMES];
    static int[] leaderNumInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] memberNumInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] leaderNumInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] memberNumInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] neetMembersArray = new int[WRITING_TIMES];
    static int[] reciprocalistsArray = new int[WRITING_TIMES];
    static int[] rationalistsArray = new int[WRITING_TIMES];
    static int[] reciprocalMembersArray = new int[WRITING_TIMES];
    static int[] finishedTasksInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] finishedTasksInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] tempTaskExecutionTimeArray = new int[WRITING_TIMES];
    static double[] taskExecutionTimeArray = new double[WRITING_TIMES];
    static int taskExecutionTimes = 0;

    static void aggregateAgentData(List<Agent> agents) {
        neetMembersArray[index] += Agent.countNEETmembers(agents, MAX_TURN_NUM / WRITING_TIMES);
		leaderNumArray[index] += countLeader(agents);
/*
        memberNumArray[index] += Agent._member_num;

        int a = Agent.countLeadersInDepopulatedArea(agents);
        int b = Agent.countLeadersInPopulatedArea(agents);

        leaderNumInDepopulatedAreaArray[index] += a;
        memberNumInDepopulatedAreaArray[index] += (Agent._lonelyAgents.size() - a);
        leaderNumInPopulatedAreaArray[index]   += b;
        memberNumInPopulatedAreaArray[index]   += (Agent._lonelyAgents.size() - b);
// */
    }

    private static int countLeader( List<Agent> agentList ) {
    	return (int) agentList.stream()
			.filter( agent -> agent.role == LEADER )
			.count();
    }

	private static int countMember( List<Agent> agentList ) {
		return (int) agentList.stream()
			.filter( agent -> agent.role == MEMBER )
			.count();
	}


	private static int countReciprocalMember( List<Agent> agentList ) {
		return (int) agentList.stream()
			.filter( agent -> agent.principle == RECIPROCAL )
			.count();
	}


    static void aggregateData(int ft, int dt, int ot, int rm, List<Agent> agentList) {
        finishedTasksArray[index] += ft;
        int priorMessages = index > 0 ? messagesArray[index - 1] : 0;
        messagesArray[index] += TransmissionPath.getMessageNum() - priorMessages;
        communicationDelayArray[index] += TransmissionPath.getAverageCommunicationTime();
        disposedTasksArray[index] += dt;
        overflownTasksArray[index] += ot;
        reciprocalMembersArray[index] += rm;
        reciprocalistsArray[index] += countReciplocalist(agentList);
/*        rationalistsArray[index] += Agent._rational_num;
        reciprocalMembersArray[index] += rm;
        finishedTasksInDepopulatedAreaArray[index] += ftida ;
        finishedTasksInPopulatedAreaArray[index]   += ftipa ;
// */
    }/**/

    static int[] leadersArray = new int[EXECUTION_TIMES];
    static int[] agentsLessThanAveArray = new int[EXECUTION_TIMES];
    static int[] leadersLessThanAveArray = new int[EXECUTION_TIMES];
    static int[] agentsLessThanQuaArray = new int[EXECUTION_TIMES];
    static int[] leadersLessThanQuaArray = new int[EXECUTION_TIMES];
    static double[] agentsExcAveArray = new double[EXECUTION_TIMES];
    static double[] leadersExcAveArray = new double[EXECUTION_TIMES];
    static double[] membersExcAveArray = new double[EXECUTION_TIMES];
    static double[]  mDependableAgentsFromAllLeaders = new double[EXECUTION_TIMES];    // 全リーダーの最終的な信頼エージェント数の平均
    static double[]  mDependableAgentsFromLeadersTrustsSomeone = new double[EXECUTION_TIMES];    // 全リーダーの最終的な信頼エージェント数の平均
    static double[] mDependableMembersFromExcellentLeader = new double[EXECUTION_TIMES];

    static void aggregateDataOnce(List<Agent> agents, int times) {
        times--;
        int leadersTrustSomeone = 0;
        int excellentLeaders    = 0;
        int resCount;
        double excellence;
        for (Agent ag : agents) {
            resCount = (int) Arrays.stream( ag.resources )
                .filter( res -> res > 0 )
                .count();
            excellence = (double) Arrays.stream( ag.resources ).sum() / resCount;

            agentsExcAveArray[times] += excellence;
            if (ag.e_leader > ag.e_member) {
                leadersArray[times]++;
                if( ag.didTasksAsLeader > 100 ){
                    excellentLeaders++;
                }

                leadersExcAveArray[times] += excellence;
                int temp = 0;
                for( Agent relAg:  ag.ls.reliableMembersRanking.keySet() ){
                    if( ag.ls.reliableMembersRanking.get(relAg) > ag.threshold_for_reciprocity_as_leader ){
                        mDependableAgentsFromAllLeaders[times]++;
                        mDependableAgentsFromLeadersTrustsSomeone[times] ++;
                        if( ag.didTasksAsLeader > 100 ){
                            mDependableMembersFromExcellentLeader[times]++;
                        }
                        temp++;
                    }else{
                        if( temp > 0 ) leadersTrustSomeone++;
                        break;
                    }
                }
            } else {
                membersExcAveArray[times] += excellence;
            }
        }
        mDependableAgentsFromAllLeaders[times] /= (double)countLeader(agents);

        if( leadersTrustSomeone == 0 ){
            mDependableAgentsFromLeadersTrustsSomeone[times] = 0;
        }else {
            mDependableAgentsFromLeadersTrustsSomeone[times] /= (double) leadersTrustSomeone;
        }

        if( excellentLeaders == 0 ){
            mDependableMembersFromExcellentLeader[times] = 0;
        }else{
            mDependableMembersFromExcellentLeader[times] /= (double) excellentLeaders;
        }
        agentsExcAveArray[times] /= (double)AGENT_NUM;
        leadersExcAveArray[times] /= (double)leadersArray[times];
        membersExcAveArray[times] /= (double)(AGENT_NUM - leadersArray[times]);



        // agentをexcellenceごとにソート
        List<Agent> temp = new ArrayList<>();
        for (Agent a : agents) {
            temp.add(a.clone());
        }
        System.out.println();
        Collections.sort(temp, new Agent.AgentExcellenceComparator());

        // 平均及び四分位点以下のexcellenceのエージェントを集計する
        int temp2 = 0;
        double quartile = Integer.MAX_VALUE, ave = Integer.MAX_VALUE;
        for (Agent ag : temp) {
            resCount = (int) Arrays.stream( ag.resources )
                .filter( res -> res > 0 )
                .count();
            excellence = (double) Arrays.stream( ag.resources ).sum() / resCount;

            temp2++;
            // 下から数えて4分の1に達したらその時のexcellenceが四分位点
            if (temp2 == AGENT_NUM / 4) quartile = excellence;
            if (temp2 == AGENT_NUM / 2) ave = excellence;
            if (excellence <= quartile) {
                agentsLessThanQuaArray[times]++;
                if (ag.e_leader > ag.e_member) {
                    leadersLessThanQuaArray[times]++;
                }
            }
            if (excellence <= ave) {
                agentsLessThanAveArray[times]++;
                if (ag.e_leader > ag.e_member) {
                    leadersLessThanAveArray[times]++;
                }
            } else {
                break;
            }
        }
        temp.clear();
    }

    static void indexIncrement() {
        if (taskExecutionTimes != 0) {
            taskExecutionTimeArray[index] += (double) tempTaskExecutionTimeArray[index] / taskExecutionTimes;
//            System.out.println(taskExecutionTimeArray[index]);
        }
        tempTaskExecutionTimeArray[index] = 0;
        taskExecutionTimes = 0;
        index = (index + 1) % WRITING_TIMES;
    }

    static void checkTask(Queue<Task> taskQueue) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.poll();
            System.out.println(temp);
            taskQueue.add(temp);
        }
        System.out.println("  Remains: " + taskQueue.size());
    }

    static void checkGrid(Agent[][] grid) {
        System.out.println("Total Agents is " + Agent._id);
        for (int i = 0; i < MAX_X; i++) {
            for (int j = 0; j < MAX_Y; j++) {
                if (grid[i][j] == null) System.out.print("    ");
                else System.out.print(String.format("%3d ", grid[i][j].id));
            }
            System.out.println();
        }
    }

    static void checkDelay(int[][] delays) {
        int[] countDelay = new int[MAX_DELAY];
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print("ID: " + i + "...");
            for (int j = 0; j < AGENT_NUM; j++) {
                System.out.print(delays[i][j] + ", ");
                if( i != j ) {
                    System.out.println(i + ", " + j + ", " + delays[i][j]);
                    countDelay[delays[i][j] - 1]++;
                }
            }
            System.out.println();
        }
        for(int i = 0; i < MAX_DELAY ;i++){
            System.out.println( (i+1) + ", " + countDelay[i]/AGENT_NUM);
        }
    }

    static void checkAgent(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);
        System.out.println("Total Agents is " + agents.size());
        System.out.println("Leaders is " + countLeader(agents) + ", Members is " + countLeader(agents));

        int recipro = 0;
        for (Agent agent : agents) {
            System.out.print("ID: " + agent.id + " Resources : ");
            for (int i = 0; i < RESOURCE_TYPES; i++) System.out.print(agent.resources[i] + ", ");
//            System.out.println(" Reliable Agents: " + agent.relAgents.size());
            System.out.println("Threshold: " + agent.threshold_for_reciprocity_as_member);
            System.out.println("Principle: " + agent.principle);
            if( agent.principle == RECIPROCAL && agent.e_member > agent.e_leader) {
                for( int wwam: agent.workWithAsM ){
                    if(wwam != 0) System.out.println("wwam:" + wwam);
                }
                recipro++;
            }
        }
        System.out.println(recipro);
    }

    static void writeResults(String st) {
        String outputFilePath = _singleton.setPath("results", st, "csv");
        System.out.println("writing now");
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        try {
            fw = new FileWriter(outputFilePath, false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.println("turn" + ", "
                            + "FinishedTasks" + ", " + "DisposedTasks" + ", " + "OverflownTasks" + ", "
                            + "Success rate(except overflow)" + ", " + "Success rate" + ", "
                            + "CommunicationTime" + ", " + "Messages" + ", " + "ExecutionTime" + ","
                            + "Leader" + ", " // + "Member"                            + ", "
                            + "NEET Members" + ", "
                            // + "Lonely leaders"                    + ", " + "Lonely members"                    + ", "
                            // + "Accompanied leaders"               + ", " + "Accompanied members"               + ", "
                            + "ReciprocalLeaders" + ", " + "ReciprocalMembers" + ", "
                            // + "Rational"                          + ", " + "ReciprocalMembers" + ","
                    // + "FinishedTasks in depopulated area" + ", " + "FinishedTasks in populated area"   + ", "
            );
            for (int i = 0; i < WRITING_TIMES; i++) {
                pw.println((i + 1) * (MAX_TURN_NUM / WRITING_TIMES) + ", "
                                + finishedTasksArray[i] / EXECUTION_TIMES + ", "
                                + disposedTasksArray[i] / EXECUTION_TIMES + ", "
                                + overflownTasksArray[i] / EXECUTION_TIMES + ", "
                                + (double) finishedTasksArray[i] / (finishedTasksArray[i] + disposedTasksArray[i]) + ", "
                                + (double) finishedTasksArray[i] / (finishedTasksArray[i] + disposedTasksArray[i] + overflownTasksArray[i]) + ", "
                                + (double)communicationDelayArray[i] / EXECUTION_TIMES + ", "
                                + (double) messagesArray[i] / EXECUTION_TIMES + ", "
                                + (double) taskExecutionTimeArray[i] / EXECUTION_TIMES + ", "
                                + (double)leaderNumArray[i] / EXECUTION_TIMES + ", "
                                + (double)neetMembersArray[i] / EXECUTION_TIMES + ", "
                                + (double)(reciprocalistsArray[i] - reciprocalMembersArray[i]) / EXECUTION_TIMES + ", "
                                + (double)reciprocalMembersArray[i] / EXECUTION_TIMES + ", "
                        /*                    + memberNumArray[i]                      / EXECUTION_TIMES + ", "
                    + leaderNumInDepopulatedAreaArray[i]     / EXECUTION_TIMES + ", "
                    + memberNumInDepopulatedAreaArray[i]     / EXECUTION_TIMES + ", "
                    + leaderNumInPopulatedAreaArray[i]       / EXECUTION_TIMES + ", "
                    + memberNumInPopulatedAreaArray[i]       / EXECUTION_TIMES + ", "
                    + rationalistsArray[i]                   / EXECUTION_TIMES + ", "
                    + reciprocalMembersArray[i]              / EXECUTION_TIMES + ", "
                    + finishedTasksInDepopulatedAreaArray[i] / EXECUTION_TIMES + ", "
                    + finishedTasksInPopulatedAreaArray[i]   / EXECUTION_TIMES + ", "
// */
                );
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeResultsX(String st) throws FileNotFoundException, IOException {
        String outputFilePath = _singleton.setPath("results", st, "xlsx");

        try {
            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("Times New Roman");
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
            int rowNumber = 0;
            int colNumber = 0;

            Sheet sheet;

            sheet = book.createSheet();
            if (sheet instanceof SXSSFSheet) {
                ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
            }
            //シート名称の設定
            book.setSheetName(0, st);

            row = sheet.createRow(rowNumber);
            _singleton.writeCell(row, colNumber++, style_header, "Turn");
            _singleton.writeCell(row, colNumber++, style_header, "Finished tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Disposed tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Overflown tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Communication time");
            _singleton.writeCell(row, colNumber++, style_header, "Execution time");
            _singleton.writeCell(row, colNumber++, style_header, "Leaders");
            _singleton.writeCell(row, colNumber++, style_header, "NEET member");

            //ウィンドウ枠の固定
            sheet.createFreezePane(1, 1);

            //ヘッダ行にオートフィルタの設定
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colNumber));

            //列幅の自動調整
            for (int j = 0; j <= colNumber; j++) {
                sheet.autoSizeColumn(j, true);
            }

            // 結果を書き込んでいく
            for (int wt = 0; wt < WRITING_TIMES; wt++) {
                rowNumber++;
                colNumber = 0;
                row = sheet.createRow(rowNumber);

                _singleton.writeCell(row, colNumber++, style_int, (wt + 1) * (MAX_TURN_NUM / WRITING_TIMES));
                _singleton.writeCell(row, colNumber++, style_int, finishedTasksArray[wt] / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, disposedTasksArray[wt] / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, overflownTasksArray[wt] / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_double, communicationDelayArray[wt] / (double) EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_double, (double) taskExecutionTimeArray[wt] / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, leaderNumArray[wt] / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, neetMembersArray[wt] / EXECUTION_TIMES);

                //列幅の自動調整
                for (int k = 0; k <= colNumber; k++) {
                    sheet.autoSizeColumn(k, true);
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

    static void writeGraphInformationX(List<Agent> agents, String st ) throws IOException {
        String outputFilePath = _singleton.setPath("relationships", st, "xlsx");
        Edge edge = new Edge();
        edge.makeEdge(agents);
        try {
            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("Times New Roman");
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
            int colNumber;

            Sheet sheet;

            for (int i = 0; i < 3; i++) {
                sheet = book.createSheet();
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                //シート名称の設定
                if (i == 0) book.setSheetName(i, "nodes");
                else if (i == 1) book.setSheetName(i, "edges");
                else if (i == 2) book.setSheetName(i, "reciprocalEdges");

                //ヘッダ行の作成
                rowNumber = 0;
                colNumber = 0;

                row = sheet.createRow(rowNumber);

                if (i == 0) _singleton.writeCell(row, colNumber++, style_header, "Node id");
                else _singleton.writeCell(row, colNumber++, style_header, "main.research.graph.Edge id");

                if (i == 0) _singleton.writeCell(row, colNumber++, style_header, "Node color");
                else _singleton.writeCell(row, colNumber++, style_header, "Source Node id");


                if (i == 0) _singleton.writeCell(row, colNumber++, style_header, "Node shape");
                else _singleton.writeCell(row, colNumber++, style_header, "Target Node id");

                if (i == 0) {
                    _singleton.writeCell(row, colNumber++, style_header, " x-coordinate ");
                    _singleton.writeCell(row, colNumber++, style_header, " y-coordinate ");
                    _singleton.writeCell(row, colNumber++, style_header, " leader id");
                    _singleton.writeCell(row, colNumber++, style_header, " delay to leader ");
//                    _singleton.writeCell(row, colNumber++, style_header, " is lonely or not");
//                    _singleton.writeCell(row, colNumber++, style_header, " is accompanied or not");
                    for (int j = 0; j < RESOURCE_TYPES; j++) {
                        _singleton.writeCell(row, colNumber++, style_header, " Resources " + j);
                        _singleton.writeCell(row, colNumber++, style_header, " Required " + j);
                        _singleton.writeCell(row, colNumber++, style_header, " Allocated " + j);
                    }
                    _singleton.writeCell(row, colNumber++, style_header, " Excellence ");
                    _singleton.writeCell(row, colNumber++, style_header, "Did tasks as leader in last period");
                    _singleton.writeCell(row, colNumber++, style_header, "Did tasks as member in last period");
                    _singleton.writeCell(row, colNumber++, style_header, " e_leader");
                    _singleton.writeCell(row, colNumber++, style_header, " e_member ");
                } else {
                    _singleton.writeCell(row, colNumber++, style_header, " length ");
                    _singleton.writeCell(row, colNumber++, style_header, " times ");
                    _singleton.writeCell(row, colNumber++, style_header, "Target Node id again");
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
                    for (Agent agent : agents) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        _singleton.writeCell(row, colNumber++, style_int, agent.id);


                        if (agent.e_leader > agent.e_member) {
                            _singleton.writeCell(row, colNumber++, style_string, "Red");
                            _singleton.writeCell(row, colNumber++, style_string, "Circle");
                        } else if (agent.principle == RATIONAL) {
                            _singleton.writeCell(row, colNumber++, style_string, "Green");
                            _singleton.writeCell(row, colNumber++, style_string, "Square");
                        } else if (agent.principle == RECIPROCAL) {
                            _singleton.writeCell(row, colNumber++, style_string, "Blue");
                            _singleton.writeCell(row, colNumber++, style_string, "Triangle");
                        }

                        _singleton.writeCell(row, colNumber++, style_int, agent.getX() * 10);
                        _singleton.writeCell(row, colNumber++, style_int, agent.getY() * 10);

                        for (int j = 0; j < RESOURCE_TYPES; j++) {
                            _singleton.writeCell(row, colNumber++, style_int, agent.resources[j]);
                            _singleton.writeCell(row, colNumber++, style_int, agent.required[j]);
                            if (agent.ls.reliableMembersRanking.size() > 0)
                                _singleton.writeCell(row, colNumber++, style_int, agent.allocated[agent.ls.reliableMembersRanking.keySet().iterator().next().id][j]);
                            else _singleton.writeCell(row, colNumber++, style_int, -1);
                        }
                        int resCount = (int) Arrays.stream( agent.resources )
                            .filter( resource -> resource > 0 )
                            .count();
                        double excellence = (double) Arrays.stream(agent.resources).sum() / resCount;
                        _singleton.writeCell(row, colNumber++, style_double, excellence);
                        _singleton.writeCell(row, colNumber++, style_int, agent.didTasksAsLeader);
                        _singleton.writeCell(row, colNumber++, style_int, agent.didTasksAsMember);
                        _singleton.writeCell(row, colNumber++, style_double, agent.e_leader);
                        _singleton.writeCell(row, colNumber++, style_double, agent.e_member);


                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
                // edgeシートへの書き込み
                else if (i == 1) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        _singleton.writeCell(row, colNumber++, style_int, j);
                        _singleton.writeCell(row, colNumber++, style_int, edge.from_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.delays.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.times.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));


                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                } else if (i == 2) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        if (edge.isRecipro.get(j) == true) {
                            rowNumber++;
                            colNumber = 0;
                            row = sheet.createRow(rowNumber);
                            _singleton.writeCell(row, colNumber++, style_int, j);
                            _singleton.writeCell(row, colNumber++, style_int, edge.from_id.get(j));
                            _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));
                            _singleton.writeCell(row, colNumber++, style_int, edge.delays.get(j));
                            _singleton.writeCell(row, colNumber++, style_int, edge.times.get(j));
                            _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));
                        }
                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
            }


            // ここからリーダーからのエッジ生成
            edge.reset();
            edge.makeEdgesFromLeader(agents);
            for (int i = 0; i < 2; i++) {
                sheet = book.createSheet();
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                if (i == 0) book.setSheetName(i + 3, "EdgesFromLeader");
                else book.setSheetName(i + 3, "reciprocalEdgesFromLeader");

                //ヘッダ行の作成
                rowNumber = 0;
                colNumber = 0;

                row = sheet.createRow(rowNumber);

                _singleton.writeCell(row, colNumber++, style_header, "main.research.graph.Edge id");
                _singleton.writeCell(row, colNumber++, style_header, "Source Node id");
                _singleton.writeCell(row, colNumber++, style_header, "Target Node id");
                _singleton.writeCell(row, colNumber++, style_header, " length ");
                _singleton.writeCell(row, colNumber++, style_header, " times ");
                _singleton.writeCell(row, colNumber++, style_header, "Target Node id again");

                //ウィンドウ枠の固定
                sheet.createFreezePane(1, 1);

                //ヘッダ行にオートフィルタの設定
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colNumber));

                //列幅の自動調整
                for (int j = 0; j <= colNumber; j++) {
                    sheet.autoSizeColumn(j, true);
                }

                if (i == 0) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        _singleton.writeCell(row, colNumber++, style_int, j);
                        _singleton.writeCell(row, colNumber++, style_int, edge.from_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.delays.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.times.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));


                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                } else if (i == 1) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        if (edge.isRecipro.get(j) != true) continue;
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        _singleton.writeCell(row, colNumber++, style_int, j);
                        _singleton.writeCell(row, colNumber++, style_int, edge.from_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.delays.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.times.get(j));
                        _singleton.writeCell(row, colNumber++, style_int, edge.to_id.get(j));

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
        edge = null;
    }

    static void writeDelays(int[][] delays) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        try {
            String currentPath = System.getProperty("user.dir");
            Date date = new Date();
            SimpleDateFormat sdf1 = new SimpleDateFormat(",yyyy:MM:dd,HH:mm:ss");
            fw = new FileWriter(currentPath + "/out/results/communicationDelay=" + MAX_DELAY + sdf1.format(date) + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

/*            pw.println("delay" + ", " + " count ");
            for (int i = 1; i < MAX_DELAY + 1; i++) {
                pw.println(i + ", " + dCounts[i] / 2);
            }
            pw.println();
// */
            pw.print("id");
            for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
            pw.println();
            for (int i = 0; i < AGENT_NUM; i++) {
                pw.print(i + ", ");
                for (int j = 0; j < AGENT_NUM; j++) {
                    pw.print(delays[i][j] + ", ");
                }
                pw.println();
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeDelaysAndRels(int[][] delays, List<Agent> agents, String st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st;

        try {
            String currentPath = System.getProperty("user.dir");
            Date date = new Date();
            SimpleDateFormat sdf1 = new SimpleDateFormat(",yyyy:MM:dd,HH:mm:ss");
            fw = new FileWriter(currentPath + "/out/results/d&r " + fileName + sdf1.format(date) + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            for (int from = 0; from < AGENT_NUM; from++) {
                for (int to = 0; to < AGENT_NUM; to++) {
//                    pw.println(delays[from][to] + ", " + agents.get(from).relRanking[to]);
                }
            }

/*            List[] delayLists = new ArrayList[MAX_DELAY + 1];
            for( int i = 0; i < delayLists.length; i++ ){
                delayLists[i] = new ArrayList<>();
            }

            for(int from = 0; from < AGENT_NUM; from++){
                for( int to = 0; to < AGENT_NUM; to++ ){
                    delayLists[delays[from][to]].add(agents.get(from).relRanking[to]);
                }
            }
            System.out.println(delayLists.length);
            for( int i = 1; i < delayLists.length ; i++ ){
                pw.print( i  );
                for( int j = 0; j < delayLists[i].size(); j++ ){
                    pw.print( ", " + delayLists[i].get(j) );
                }
                pw.println();
            }
            // */
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeReliabilities(List<Agent> agents, String st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st;
        try {
            String currentPath = System.getProperty("user.dir");
            Date date = new Date();
            SimpleDateFormat sdf1 = new SimpleDateFormat(",yyyy:MM:dd,HH:mm:ss");
            fw = new FileWriter(currentPath + "/out/results/rel" + fileName + ", λ=" + String.format("%.2f", ADDITIONAL_TASK_NUM ) + sdf1.format(date) + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            // 列番号入れる部分
            pw.print(" , targets");
            for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i + ", ");
            pw.println();

            pw.print(", Role, ");
            for (int i = 0; i < AGENT_NUM; i++) pw.print("             DE_l ,             DE_m, " );
            pw.println();

            for (Agent from : agents) {
                // column 1 : Role
                pw.print(from.id + ", ");
                if (from.e_member > from.e_leader) {
                    pw.print("Member, ");
                } else {
                    pw.print("Leader, ");
                }

                // column 2~3 :  DE_l(from→target), DE_m(from→target),
                for ( Agent target: agents) {
                    // 自分は飛ばす
                    if( target.equals(from) ){
                        pw.print(" , ,");
                    }else{
                        pw.print(from.ls.reliableMembersRanking.get(target) + ", ");
                        pw.print(from.ms.reliableLeadersRanking.get(target) + ", ");
                    }
                }
                pw.println();
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
// */

/*        pw.print("I'm 4 "+ ", ");
        for (int i = 0; i < AGENT_NUM; i++) pw.print(agents.get(4).relRanking[i] + ", ");
        pw.println();
// */
    }

    static int countReciplocalist(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
//            if (agent.relRanking[agent.relRanking.get(0).id] > agent.threshold_for_reciprocity_as_member
//                    && (agent.e_member > THRESHOLD_FOR_ROLE_RECIPROCITY
//                    || agent.e_leader > THRESHOLD_FOR_ROLE_RECIPROCITY))
//                temp++;
        }
        return temp;
    }

    static int[] dCounts = new int[MAX_DELAY + 1];

    static void countDelays(int[][] delays) {
        for (int[] row : delays) {
            for (int column : row) {
                dCounts[column]++;
            }
        }
    }

    static int countReciplocalMembers(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
//            if (agent.relRanking[agent.relRanking.get(0).id] > agent.threshold_for_reciprocity_as_member
//                    && agent.e_member > THRESHOLD_FOR_ROLE_RECIPROCITY
//                    && agent.e_member > agent.e_leader)
//                temp++;
        }
        return temp;
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

    private String setPath(String dir_name, String file_name, String extension) {
        String currentPath = System.getProperty("user.dir");
        Date date = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat(",yyyy_MM_dd,HH_mm_ss");
        System.out.println("Writing on " + dir_name + "/" + file_name + ",λ=" + String.format("%.2f", ADDITIONAL_TASK_NUM ) + sdf1.format(date) + "." + extension);
        return currentPath + "/out/" + dir_name + "/" + file_name + ",λ=" + String.format("%.2f",  ADDITIONAL_TASK_NUM ) + sdf1.format(date) + "." + extension;
    }

    private void prepareExcelSheet() {

    }

    // あるrowのcolumn列にoを書き込むメソッド
    // 返り値は
    private Cell writeCell(Row row, int col_number, CellStyle style, Object o) {
        Cell cell;
        if (o.getClass().getName() == "java.lang.String") {
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellValue(o.toString());
        } else if (o.getClass().getName() == "java.lang.Integer") {
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellValue((int) o);
        } else {
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellValue((double) o);
        }
        return cell;
    }

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
