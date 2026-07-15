package com.qq.ijay997;

public class Test {
    public static void main(String[] args) {
        determineSuit(Standard.SPADE);
        determineSuitQualifiedNames(Standard.SPADE);
    }

    static void error(Object obj) {
        switch(obj) {
            case String s -> // error: this case label is dominated by a preceding case label
                    System.out.println("A string: " + s);
            case CharSequence cs ->
                    System.out.println("A sequence of length " + cs.length());
            default -> { break; }
        }
    }

    static void determineSuit(CardClassification c) {
        switch (c) {
            case Standard s when s == Standard.SPADE -> System.out.println("Spades");
            case Standard s when s == Standard.HEART -> System.out.println("Hearts");
            case Standard s when s == Standard.DIAMOND -> System.out.println("Diamonds");
            case Standard s -> System.out.println("Clubs");
            case Tarot t when t == Tarot.SPADE -> System.out.println("Spades or Piques");
            case Tarot t when t == Tarot.HEART -> System.out.println("Hearts or C\u0153ur");
            case Tarot t when t == Tarot.DIAMOND -> System.out.println("Diamonds or Carreaux");
            case Tarot t when t == Tarot.CLUB -> System.out.println("Clubs or Trefles");
            case Tarot t when t == Tarot.TRUMP -> System.out.println("Trumps or Atouts");
            case Tarot t -> System.out.println("The Fool or L'Excuse");
        }
    }



    static void determineSuitQualifiedNames(CardClassification c) {
        switch (c) {
            case Standard.SPADE -> System.out.println("黑桃");
            case Standard.HEART -> System.out.println("红桃");
            case Standard.DIAMOND -> System.out.println("方块");
            case Standard.CLUB -> System.out.println("梅花");
            case Tarot.SPADE -> System.out.println("黑桃或皮克牌");
            case Tarot.HEART -> System.out.println("红桃或库尔牌");
            case Tarot.DIAMOND -> System.out.println("方块或卡勒牌");
            case Tarot.CLUB -> System.out.println("梅花或特雷夫牌");
            case Tarot.TRUMP -> System.out.println("王牌或阿托牌");
            case Tarot.EXCUSE -> System.out.println("愚者或借口牌");
        }
    }
}
