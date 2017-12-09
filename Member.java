public class Member implements Role {
    private static Member member = new Member();
    private Member(){
    }
    public static Member getInstance(){
        return member;
    }

}
