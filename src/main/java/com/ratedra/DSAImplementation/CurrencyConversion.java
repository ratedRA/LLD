package com.ratedra.DSAImplementation;


import java.util.*;

class Currency{
    String source;
    String target;
    Double rate;

    public Currency(String source, String target, Double rate) {
        this.source = source;
        this.target = target;
        this.rate = rate;
    }

    @Override
    public String toString() {
        return target+"@: "+rate;
    }
}

public class CurrencyConversion {
    public static void main(String[] args) {
        Currency c1=new Currency("USD","JPY",110.0);
        Currency c2=new Currency("USD","AUD",1.45);
        Currency c3=new Currency("JPY","GBP",0.0070);

        Currency[] curr=new Currency[3];
        curr[0]=c1;
        curr[1]=c2;
        curr[2]=c3;

        Map<String, List<Currency>> graph = new HashMap<>();
        for(Currency currency : curr){
            graph.computeIfAbsent(currency.source, k -> new ArrayList<>()).add(currency);
            graph.computeIfAbsent(currency.target, k -> new ArrayList<>()).add(new Currency(currency.target, currency.source, 1/ currency.rate));
        }

        System.out.println(graph);

        Double exchangeRate = getExchangeRate("GBP", "AUD", graph);
        System.out.println(exchangeRate);
    }

    private static Double getExchangeRate(String source, String target, Map<String, List<Currency>> graph){
        Queue<String> q = new LinkedList<>();
        Queue<Double> val = new LinkedList<>();
        val.offer(1.0);
        q.offer(source);
        Set<String> visited = new HashSet<>();
        visited.add(source);
        while(!q.isEmpty()){
            String node = q.poll();
            Double conversion = val.poll();
            for(Currency neighbour : graph.get(node)){
                if(!visited.contains(neighbour.target)){
                    q.offer(neighbour.target);
                    val.offer(conversion*neighbour.rate);
                    if(Objects.equals(neighbour.target, target)){
                        return val.poll();
                    }
                    visited.add(neighbour.target);
                }
            }
        }
        return null;
    }
}
