package nl.alfaone.domain;

import com.embabel.agent.api.common.SomeOf;

/**
 * Starting input for the employee query processing flow.
 * Implements SomeOf to bind to the blackboard in Embabel's GOAP system.
 * This is the entry point that triggers the agent run.
 */
public record QueryInput(String query) implements SomeOf {
}
