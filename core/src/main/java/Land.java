
public class Land {
    private String name;
    private long width;
    private long height;

    public Land(String name){
        this.name = name;
        this.width = 100;
        this.height = 100;
    }
    public String getName(){
        return this.name;
    }
    public long getSize(){
        return this.width * this.height;
    }
}
