package com.example.demo;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVUtilTest {

    @Test
    void converterData(){
        List<Player> list = CsvUtilFile.getPlayers();
        assert list.size() == 18207;
    }

    @Test
    void stream_filtrarJugadoresMayoresA35(){
        List<Player> list = CsvUtilFile.getPlayers();
        Map<String, List<Player>> listFilter = list.parallelStream()
                .filter(player -> player.age >= 35)
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .flatMap(playerA -> list.parallelStream()
                        .filter(playerB -> playerA.club.equals(playerB.club))
                )
                .distinct()
                .collect(Collectors.groupingBy(Player::getClub));

        assert listFilter.size() == 322;
    }


    @Test
    void reactive_filtrarJugadoresMayoresA35(){
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age >= 35)
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .buffer(100)
                .flatMap(playerA -> listFlux
                        .filter(playerB -> playerA.stream()
                                .anyMatch(a ->  a.club.equals(playerB.club)))
                )
                .distinct()
                .collectMultimap(Player::getClub);

        assert listFilter.block().size() == 322;
    }


    @Test
    void reactive_nacionalidad() {
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<String> listFlux = Flux.fromStream(list.parallelStream()).map(player -> {
            return player.national;
        }).sort().cache();
        Mono<List<String>> nacionalidades = listFlux.collectList();
        System.out.println(nacionalidades.block());
        assert  nacionalidades.block().size() == 164;

    }

    @Test
    void reaactive_ranking_jugadores() {
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Flux<String> nacionalidades = Flux.fromStream(list.parallelStream()).map(player -> {
            return player.national;
        }).distinct().cache();
        var filtro = nacionalidades
                .flatMap(
                        nacionalidad -> {
                            return listFlux.filter(player -> player.national.equals(nacionalidad))
                                    .sort((obj1, obj2) -> obj1.compareWinners(obj2)).take(3);
                        }
                );
        Mono<List<Player>> rank =filtro.collectList();
        System.out.println(rank.block().get(0).name);
        System.out.println(rank.block().get(100).name);
        System.out.println(rank.block().get(200).name);
    }

}
