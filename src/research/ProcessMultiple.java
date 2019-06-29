package research;

import research.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

public class ProcessMultiple implements Runnable{
    private static ProcessMultiple _singleton = new ProcessMultiple();
    static List<Strategy> stList = new ArrayList<>();

    private ProcessMultiple(){
    }

    public boolean processStrategy(Strategy st){
        stList.add(st);
        ProcessMultiple pm = new ProcessMultiple();
        Thread          th = new Thread(pm);
        th.start();
        System.out.println( st.getClass().getName() + ": 終了待ち" );
        try{
            th.join();
        } catch(InterruptedException e){
            System.out.println(e);
            return false;
        }
        System.out.println( st.getClass().getName() + ": 完了" );
        return true;
    }

    @Override
    public void run(){
        System.out.println(stList.get(0).getClass().getName() + " now");
    }

    public static ProcessMultiple getInstance(){
        return _singleton;
    }
}
