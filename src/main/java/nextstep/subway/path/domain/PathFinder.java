package nextstep.subway.path.domain;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import nextstep.subway.line.domain.ExtraFare;
import nextstep.subway.line.domain.Line;
import nextstep.subway.station.domain.Station;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.WeightedMultigraph;

public class PathFinder {

    private final static String STATION_SAME_ERROR = "출발역과 도착역이 서로 동일합니다.";

    private final static String STATION_NOT_CONNECT_ERROR = "출발역과 도착역이 연결되지 않았습니다.";

    private final static String NOT_EXIST_STATION_ERROR = "지하철 역이 존재하지 않습니다.";

    private final WeightedMultigraph<Station, SectionEdge> graph = new WeightedMultigraph<>(SectionEdge.class);

    private PathFinder(List<Line> lines) {
        lines.forEach(line -> {
            line.getStations()
                .forEach(graph::addVertex);
            line.getSections()
                .getSections()
                .forEach(section -> {
                    SectionEdge sectionEdge = new SectionEdge(section);
                    graph.addEdge(section.getUpStation(), section.getDownStation(), sectionEdge);
                    graph.setEdgeWeight(sectionEdge, section.getDistance().value());
                });
        });
    }

    public static PathFinder from(List<Line> lines) {
        return new PathFinder(lines);
    }

    public Path findShortestPath(Station sourceStation, Station targetStation, int age) {
        validateSameStation(sourceStation, targetStation);
        validateNotExistStation(sourceStation, targetStation);
        DijkstraShortestPath<Station, SectionEdge> dijkstraShortestPath = new DijkstraShortestPath<>(graph);
        GraphPath<Station, SectionEdge> shortestPath = dijkstraShortestPath.getPath(sourceStation, targetStation);
        validateNotConnect(shortestPath);
        List<Station> shortestPathVertexes = shortestPath.getVertexList();
        double shortestPathWeight = shortestPath.getWeight();
        return Path.of(shortestPathVertexes, (int) shortestPathWeight, getLineExtraFare(shortestPath), age);
    }

    private void validateSameStation(Station sourceStation, Station targetStation) {
        if (sourceStation.equals(targetStation)) {
            throw new IllegalArgumentException(STATION_SAME_ERROR);
        }
    }

    private void validateNotConnect(GraphPath<Station, SectionEdge> shortestPath) {
        if (Objects.isNull(shortestPath)) {
            throw new IllegalArgumentException(STATION_NOT_CONNECT_ERROR);
        }
    }

    private void validateNotExistStation(Station sourceStation, Station targetStation) {
        if (isContainsStation(sourceStation, targetStation)) {
            throw new IllegalArgumentException(NOT_EXIST_STATION_ERROR);
        }
    }

    private boolean isContainsStation(Station sourceStation, Station targetStation) {
        return !graph.containsVertex(sourceStation) || !graph.containsVertex(targetStation);
    }

    private int getLineExtraFare(GraphPath<Station, SectionEdge> shortestPath) {
        return shortestPath.getEdgeList()
            .stream()
            .map(SectionEdge::getLine)
            .collect(Collectors.toList())
            .stream()
            .map(line -> line.getExtraFare().value())
            .mapToInt(x -> x)
            .max()
            .getAsInt();
    }
}
