public class Leader implements Role {
    private static Leader leader = new Leader();
    private Leader(){ }
    public static Role getInstance(){
        return leader;
    }

}
