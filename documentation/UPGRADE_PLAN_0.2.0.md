# Embabel 0.2.0 Upgrade Plan

## Executive Summary

**Objective**: Upgrade from Embabel 0.1.4 to 0.2.0 to enable LLM-driven tool selection using `@Tool` annotations.

**Current State**:
- ✅ Working system with keyword-based routing
- ✅ Multiple @Action methods for different queries
- ❌ No intelligent tool selection by LLM

**Target State**:
- ✅ LLM automatically decides which tools to call
- ✅ Support for complex multi-criteria queries
- ✅ More flexible and extensible query handling

**Risk Level**: 🟡 **MEDIUM**
- Embabel 0.2.0 is newer, potential breaking changes
- API might have changed between versions
- Need to test thoroughly

**Estimated Time**: 2-4 hours

---

## Phase 1: Research & Preparation (30 min)

### 1.1 Verify 0.2.0 Availability
```bash
# Check if version 0.2.0 exists in Embabel repository
mvn versions:display-dependency-updates -DincludeGroupIds=com.embabel.agent
```

### 1.2 Check Release Notes
- [ ] Review Embabel 0.2.0 release notes
- [ ] Identify breaking changes
- [ ] Note deprecated features
- [ ] Check compatibility with Spring Boot 3.5.7 and Java 21

### 1.3 Create Backup
```bash
git checkout -b embabel-0.2.0-upgrade
git add .
git commit -m "Backup before Embabel 0.2.0 upgrade"
```

---

## Phase 2: Dependency Update (15 min)

### 2.1 Update pom.xml

**Current**:
```xml
<properties>
    <embabel-agent.version>0.1.4</embabel-agent.version>
</properties>
```

**New**:
```xml
<properties>
    <embabel-agent.version>0.2.0</embabel-agent.version>
</properties>
```

### 2.2 Update Dependencies (if needed)

Check if dependency structure changed:
```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

### 2.3 Initial Build Test
```bash
cd alfa-wall-addon
mvn clean compile
```

**Expected**: Compilation errors due to API changes

---

## Phase 3: Code Migration (1-2 hours)

### 3.1 Update EmployeeCollectorAgent

**Before (0.1.4 - Keyword Routing)**:
```java
@Action
public List<Employee> collectEmployees(String query, OperationContext context) {
    String lowerQuery = query.toLowerCase();

    if (lowerQuery.contains("here")) {
        return getCurrentPresence();
    }

    if (lowerQuery.contains("java")) {
        return filterBySkill("java");
    }

    // ... more keyword routing
}
```

**After (0.2.0 - LLM Tool Selection)**:
```java
@Action
public List<Employee> collectEmployees(String query, OperationContext context) {
    log.info("Collecting employees for query: '{}'", query);

    // LLM analyzes query and automatically calls appropriate tools
    return context.ai().withDefaultLlm()
            .withTools(this) // Register all @Tool methods
            .createObject(
                    "Analyze this employee query and use available tools to find matching employees: '" + query + "'. " +
                    "Instructions:\n" +
                    "- Use semantic search for general queries about skills or background\n" +
                    "- Use exact filters for specific criteria (customer names, parking, schedule)\n" +
                    "- For complex queries requiring multiple criteria (e.g., 'Java developers coming today'), " +
                    "  call multiple tools and use intersectEmployeeLists to combine results\n" +
                    "- Return the final list of matching employees",
                    new TypeReference<List<Employee>>() {}
            );
}
```

### 3.2 Convert @Action to @Tool

**For each data source method**, change from `@Action` to `@Tool` with description:

```java
// BEFORE
@Action
public List<Employee> getCurrentPresence() {
    log.info("Tool called: getCurrentPresence()");
    // ...
}

// AFTER
@Tool(description = "Get employees who are currently in the office (based on device trackers). " +
      "Use for queries like: 'who is here now', 'who is in the office', 'current presence'")
public List<Employee> getCurrentPresence() {
    log.info("Tool called: getCurrentPresence()");
    // ...
}
```

**Complete list to convert**:
- [x] `searchEmployees(String searchQuery)` → @Tool
- [x] `getCurrentPresence()` → @Tool
- [x] `filterBySkill(String skill)` → @Tool
- [x] `filterByCustomer(String customerName)` → @Tool
- [x] `filterBySchedule(String dateString)` → @Tool
- [x] `filterByParking(String dateString)` → @Tool
- [x] `getAllEmployees()` → @Tool
- [x] `intersectEmployeeLists(List<List<Employee>> lists)` → @Tool
- [x] `combineEmployeeLists(List<List<Employee>> lists)` → @Tool

### 3.3 Remove Keyword Routing Logic

Delete the keyword-based routing code since LLM will handle it:

```java
// DELETE THIS ENTIRE SECTION
if (lowerQuery.contains("here") || lowerQuery.contains("in the office")) {
    return getCurrentPresence();
}

if (lowerQuery.contains("coming") || lowerQuery.contains("scheduled")) {
    return filterBySchedule("today");
}

// ... etc
```

### 3.4 Add @ToolParam Annotations

For methods with parameters, add descriptions:

```java
@Tool(description = "Search employee database using text search...")
public List<Employee> searchEmployees(
    @ToolParam(description = "The search query string") String searchQuery) {
    // ...
}

@Tool(description = "Filter employees by exact skill name...")
public List<Employee> filterBySkill(
    @ToolParam(description = "Skill name like 'Java', 'Python', 'React'") String skill) {
    // ...
}
```

### 3.5 Check for API Changes

Verify if these methods still work in 0.2.0:
- [ ] `context.ai().withDefaultLlm()`
- [ ] `.withTools(this)`
- [ ] `.createObject(...)`
- [ ] `TypeReference<List<Employee>>`

---

## Phase 4: Testing (1 hour)

### 4.1 Build Test
```bash
mvn clean compile
```

**Success Criteria**: BUILD SUCCESS with no errors

### 4.2 Unit Tests

Create test for LLM tool selection:

```java
@Test
void testLlmSelectsCorrectToolForJavaQuery() {
    String query = "Who knows Java?";
    List<Employee> result = employeeCollectorAgent.collectEmployees(query, context);

    // Verify LLM called filterBySkill("Java")
    assertTrue(result.stream().allMatch(emp -> emp.hasSkill("Java")));
}

@Test
void testLlmSelectsCorrectToolForPresenceQuery() {
    String query = "Who is here now?";
    List<Employee> result = employeeCollectorAgent.collectEmployees(query, context);

    // Verify LLM called getCurrentPresence()
    assertTrue(result.stream().allMatch(Employee::isCurrentlyPresent));
}
```

### 4.3 Integration Tests

**Test 1: Simple Query**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Who knows Java?"
```

Expected:
- LLM calls `filterBySkill("Java")`
- Returns John Doe
- Purple LEDs light up

**Test 2: Natural Language Variation**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Show me Java developers"
```

Expected:
- LLM understands variation
- Still calls `filterBySkill("Java")`
- Same result as Test 1

**Test 3: Complex Multi-Criteria** (NEW CAPABILITY!)
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Who with Python skills is coming today?"
```

Expected:
- LLM calls `filterBySkill("Python")`
- LLM calls `filterBySchedule("today")`
- LLM calls `intersectEmployeeLists([pythonDevs, comingToday])`
- Returns intersection
- Purple LEDs (skill query context)

**Test 4: Ambiguous Query** (LLM handles it!)
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Find backend experts"
```

Expected:
- LLM calls `searchEmployees("backend experts")`
- Semantic search finds Java/Python/Spring Boot developers
- Returns relevant employees

### 4.4 Performance Test

Measure LLM overhead:
```bash
time curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Who knows Java?"
```

Compare:
- 0.1.4 (keyword routing): ~100ms
- 0.2.0 (LLM tool selection): ~500-2000ms (depending on LLM)

**Note**: LLM adds latency but provides much better accuracy

---

## Phase 5: Validation (30 min)

### 5.1 Query Coverage Test

Test all supported query types work with LLM:

| Query | Expected Tool | Expected Color | Status |
|-------|---------------|----------------|--------|
| "Who knows Java?" | filterBySkill | Purple | ⏳ |
| "Who is here now?" | getCurrentPresence | Green | ⏳ |
| "Who has parking?" | filterByParking | Orange | ⏳ |
| "Who works for ACME?" | filterByCustomer | Yellow | ⏳ |
| "Who is coming today?" | filterBySchedule | Blue | ⏳ |
| "Java devs coming today" | filterBySkill + filterBySchedule + intersect | Purple | ⏳ |

### 5.2 Edge Cases

- [ ] Empty results: "Who knows COBOL?"
- [ ] Unknown queries: "What's the weather?"
- [ ] Ambiguous: "Who is good with backend stuff?"
- [ ] Multiple skills: "Who knows Java or Python?"

### 5.3 Privacy Check

Ensure privacy still works:
- [ ] PII detected and sanitized
- [ ] Critical PII blocks request
- [ ] Sanitized query passed to LLM

---

## Phase 6: Documentation Update (15 min)

### 6.1 Update IMPLEMENTATION.md

- [x] Change "Current Implementation" section to 0.2.0
- [x] Update code examples to show @Tool
- [x] Add "Complex Query" examples
- [x] Note performance characteristics

### 6.2 Update README/CLAUDE.md

- [ ] Update version number
- [ ] Add note about LLM-driven tool selection
- [ ] Update query examples

---

## Rollback Plan

If upgrade fails or breaks critical functionality:

```bash
# Rollback git changes
git checkout embabel-0.1.4

# Or manually revert pom.xml
<embabel-agent.version>0.1.4</embabel-agent.version>

# Rebuild
mvn clean compile
```

**Rollback Triggers**:
- Build fails with unresolvable errors
- @Tool annotation doesn't exist in 0.2.0
- Breaking API changes in OperationContext
- LLM tool selection doesn't work
- Performance degradation > 5 seconds per query

---

## Success Criteria

✅ **Must Have**:
1. BUILD SUCCESS with 0.2.0
2. All existing queries still work
3. LLM successfully selects correct tools
4. Privacy officer still functions
5. LED visualization still works

✅ **Nice to Have**:
1. Complex multi-criteria queries work
2. Better handling of ambiguous queries
3. Natural language variations understood
4. Performance acceptable (< 3 sec per query)

---

## Risk Mitigation

### Risk 1: @Tool annotation doesn't exist
**Mitigation**: Verify in Embabel docs before starting
**Fallback**: Stay on 0.1.4

### Risk 2: Breaking API changes
**Mitigation**: Test incrementally, commit often
**Fallback**: Rollback to working commit

### Risk 3: LLM makes wrong tool selections
**Mitigation**:
- Improve @Tool descriptions
- Add examples in LLM prompt
- Fine-tune withTools() configuration
**Fallback**: Add validation layer

### Risk 4: Performance too slow
**Mitigation**:
- Cache common queries
- Use faster LLM model
- Implement timeout
**Fallback**: Keep keyword routing as fallback

---

## Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Research | 30 min | ⏳ Pending |
| Phase 2: Dependencies | 15 min | ⏳ Pending |
| Phase 3: Code Migration | 1-2 hours | ⏳ Pending |
| Phase 4: Testing | 1 hour | ⏳ Pending |
| Phase 5: Validation | 30 min | ⏳ Pending |
| Phase 6: Documentation | 15 min | ⏳ Pending |
| **Total** | **2-4 hours** | |

---

## Next Steps

**Ready to proceed?**

1. ✅ Review this plan
2. ✅ Get approval
3. ⏳ Create backup branch
4. ⏳ Start Phase 1: Research

**Questions to answer before starting:**
- Do we have access to LLM for tool selection? (OpenAI API, Anthropic, local model?)
- What's acceptable query latency? (1 sec? 3 sec? 5 sec?)
- Should we keep keyword routing as fallback if LLM fails?
- Are we okay with increased LLM API costs?

**Let me know when you're ready to start the upgrade!**
