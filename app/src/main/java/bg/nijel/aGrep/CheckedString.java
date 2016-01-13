package bg.nijel.aGrep;

public class CheckedString {
    boolean checked;
    String string;

    public CheckedString(String _s){
        this(true,_s);
    }

    public CheckedString(boolean _c,String _s){
        this.checked = _c;
        this.string = _s;
    }

    public CheckedString setChecked(boolean checked){
        return new CheckedString(checked, this.string);
    }

    public String toString(){
        return this.checked + "|" + this.string;
    }

}