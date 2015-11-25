package org.csproject.model.magic;

/**
 * Created by Nick on 10/31/2015.
 */
public class OffensiveMagic extends Magic {
    protected String element;
    protected int value;

    public OffensiveMagic(String name, String element, int v){
        super(name, "Offensive");
        this.element = element;
        this.value = v;
    }
}