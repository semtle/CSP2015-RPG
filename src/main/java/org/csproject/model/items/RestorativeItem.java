package org.csproject.model.items;

/**
 * Created by Nick on 10/31/2015.
 * The Restorative Item class.
 * Restorative items are used to restore health and mana to player characters.
 * Most items only target one character, some can target the entire team.
 */
public class RestorativeItem extends Item {
    protected boolean hasTargetPlayer;
    protected boolean hasTargetTeam;

    protected String attributeEffected; //either "Health" or "Mana"
    protected int value;

    public RestorativeItem(String name, boolean targetPlayer, boolean targetTeam, String attribute, int v, int b){
        super(name, "Restorative", b);
        this.hasTargetPlayer = targetPlayer;
        this.hasTargetTeam = targetTeam;
        this.attributeEffected = attribute;
        this.value = v;
    }

    public String getTarget(){
        if(hasTargetPlayer){ return "Player"; }
        else if(hasTargetTeam){ return "Team"; }
        else { return "No Target"; }
    }

    public String getAttribute(){
        return attributeEffected;
    }

    public int getValue(){
        return value;
    }
}
