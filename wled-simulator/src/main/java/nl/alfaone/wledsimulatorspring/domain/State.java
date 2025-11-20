package nl.alfaone.wledsimulatorspring.domain;

import lombok.Data;

import java.util.List;

@Data
public class State {
    private List<List<Integer>> col;
}
