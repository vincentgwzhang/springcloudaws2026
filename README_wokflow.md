## GitHub Actions Workflow: Complete Knowledge Base

Looking at your workflow file, let me provide comprehensive answers to all your questions in English:

---

## 1. Job Count and Execution Model

**Your workflow has 2 jobs:**
```yaml
jobs:
  build-test:        # job 1
    ...
  docker-publish:    # job 2
    ...
```

**Default execution:** Parallel running

However, you added:
```yaml
docker-publish:
  needs: build-test
```

This creates **dependency:** `docker-publish` waits for `build-test` to complete.

---

## 2. Job Dependency Options

```yaml
# Sequential execution
job-a:
  ...
job-b:
  needs: job-a        # Waits for job-a

# Multiple dependencies
job-c:
  needs: [job-a, job-b]

# No needs = parallel (independent)
job-d:
  ...
```

---

## 3. Every Job Has Steps

**Yes.** Each job contains `steps`:
```yaml
build-test:
  steps:
    - name: Checkout source code
    - name: Set up JDK 25
    - name: Run Maven clean install
```

---

## 4. Step Execution: Sequential by Default

Steps execute **sequentially** - one must complete before the next starts.

**BUT** - You asked about parallel commands within a single step. Here's the answer:

```yaml
- name: Run parallel commands
  run: |
    command1 &      # Run in background (&)
    command2 &      # Run in background (&)
    wait            # Wait for ALL background jobs to complete
```

**Breakdown:**
- `command1 &` → Starts command1 in background, immediately returns
- `command2 &` → Starts command2 in background, immediately returns
- `wait` → Blocks until both commands finish

**So:** `command1` and `command2` run **in parallel**, but the step waits for both to complete before moving to the next step. Without `wait`, the step would end immediately and potentially kill the background jobs.

---

## 5. `runs-on` Across Different Jobs

**Your question:** Are different jobs with `runs-on: ubuntu-latest` in the same environment or different?

**Answer:**
- Each job gets a **fresh, separate VM instance**
- Even if both say `ubuntu-latest`, they are **different machines**
- Persistent data from job-a is **NOT available** to job-b

**Example:**
```yaml
build-test:
  runs-on: ubuntu-latest
  steps:
    - run: echo "Build data" > /tmp/data.txt   # Saved in job-a's VM
    
docker-publish:
  needs: build-test
  runs-on: ubuntu-latest
  steps:
    - run: cat /tmp/data.txt   # FILE NOT FOUND - different VM!
```

**To share data between jobs:**
```yaml
build-test:
  runs-on: ubuntu-latest
  steps:
    - run: echo "data" > artifact.txt
    - uses: actions/upload-artifact@v4
      with:
        name: my-artifact
        path: artifact.txt

docker-publish:
  needs: build-test
  runs-on: ubuntu-latest
  steps:
    - uses: actions/download-artifact@v4
      with:
        name: my-artifact
    - run: cat artifact.txt   # NOW available
```

**To ensure SAME environment across jobs:**
```yaml
build-test:
  runs-on: ubuntu-24.04      # Specific version

docker-publish:
  runs-on: ubuntu-24.04      # Same specific version
```

**Bitbucket vs GitHub:**
Yes, **both use Docker/virtualization** for job execution:
- **GitHub Actions:** Runs jobs on GitHub-hosted VMs (Ubuntu, Windows, macOS) or self-hosted runners
- **Bitbucket Pipelines:** Similar - runs in Docker containers by default

---

## 6. Step Failure: Is It Customizable?

**Default behavior:** A failed step **immediately stops the job** - subsequent steps are skipped.

**This IS customizable:**

```yaml
- name: Maven package
  run: mvn -DskipTests package
  continue-on-error: true    # Step fails but doesn't stop job
```

When `continue-on-error: true`:
- Step can fail
- Job continues to next step
- But the step is still marked as "failed" in logs

**So:** It's not a "necessary" behavior - you can override it per step or per job.

---

## 7. Job Failure Propagation vs `if` Conditions - The Key Clarification

**Your observation:** There's an apparent contradiction. Let me clarify:

### Default behavior (without conditions):
```yaml
build-test:
  steps:
    - run: mvn clean install     # Fails

docker-publish:
  needs: build-test              # Skipped because build-test failed
  steps:
    - run: docker push           # Never runs
```

**Result:** `docker-publish` is **automatically skipped**.

### With `if` conditions at step level:
```yaml
docker-publish:
  needs: build-test
  steps:
    - name: Always checkout
      run: git checkout
      if: always()               # Runs even if build-test failed
      
    - name: Deploy
      run: docker push
      if: success()              # Only if build-test succeeded
```

**Key insight:** The `if` condition **overrides the automatic skip behavior**.

### With `if` conditions at job level:
```yaml
docker-publish:
  needs: build-test
  if: always()                   # Job itself runs even if build-test failed
  steps:
    - run: docker push
```

**So there's NO contradiction:**
- **Without conditions:** Job is automatically skipped if dependency fails
- **With conditions:** You explicitly override the skip behavior

---

## 8. Step-Level `if` Condition Scope

**Your question:** Does `if: success()` / `if: failure()` check the last step or the last job?

**Answer: It checks the LAST STEP in the CURRENT JOB, not the dependency job.**

```yaml
build-test:
  steps:
    - name: Step A
      run: ...         # Fails
    - name: Step B
      run: ...         # Skipped automatically

docker-publish:
  needs: build-test
  steps:
    - name: Step C
      run: echo "X"
      if: success()    # Checks build-test's outcome, but...
```

**Wait - I need to correct this.** Actually:

- `if: success()` at **step level** checks if **previous steps in current job** succeeded
- **NOT** the dependency job status directly

**To check dependency job status, use:**
```yaml
docker-publish:
  needs: build-test
  steps:
    - run: docker push
      if: needs.build-test.result == 'success'   # Checks job result
```

**Important distinction:**
```yaml
docker-publish:
  needs: build-test
  if: success()        # Job-level: checks if all previous jobs succeeded
  steps:
    - run: docker push
      if: success()    # Step-level: checks if previous steps succeeded
```

**Do you need `continue-on-error` on the dependency job?**

No, not for the step-level `if: success()` at the job level. The job status is automatically captured and can be queried with `needs.build-test.result`.

---

## 9. Workflow Final Status: Green vs Red

**Your question:** If a job fails but `continue-on-error: true` allows the workflow to finish, what's the final status?

**Answer:**

| Scenario | Workflow Status | Appearance |
|----------|-----------------|-----------|
| All jobs succeed | ✅ Green | Green checkmark |
| A job fails, but no one depends on it | ⚠️ Amber/Yellow | Warning icon |
| A critical job fails (blocks dependent job) | ❌ Red | Red X |
| A job fails but `continue-on-error: true` | ⚠️ Amber/Yellow | Warning icon |
| A step fails in a skipped job | ⚠️ Amber | Warning |

**The rule:**
- **Green:** All steps in all required jobs passed
- **Yellow/Amber:** Some steps failed but marked with `continue-on-error`
- **Red:** A critical path job failed (dependency chain broken)

---

## 10. `runs-on: self-hosted` Explanation

**What is `self-hosted`?**

GitHub Actions provides two types of runners:
1. **GitHub-hosted runners** (default)
   - Ubuntu, Windows, macOS VMs provided by GitHub
   - `runs-on: ubuntu-latest`, `windows-latest`, etc.
   - Cloud-based, ephemeral

2. **Self-hosted runners**
   - Your own machine/server connected to GitHub
   - `runs-on: self-hosted`
   - Persistent, you manage the environment

**Default:**
```yaml
runs-on: ubuntu-latest    # Default: GitHub-hosted
```

**When to use `self-hosted`:**
- Need specific hardware (GPU, specialized CPU)
- Need to access internal private networks
- Need persistent data between runs
- Cost optimization (after scale)

**Architecture comparison:**
```
GitHub-hosted:                   Self-hosted:
┌─────────────────────┐         ┌──────────────────┐
│ GitHub Cloud        │         │ Your Server      │
│  ┌──────────────┐   │         │  ┌────────────┐  │
│  │ Ubuntu VM    │   │  ←→     │  │ Runner App │  │
│  │ (Ephemeral)  │   │         │  │ (Persistent)   │
│  └──────────────┘   │         │  └────────────┘  │
└─────────────────────┘         └──────────────────┘
```

---

## 11. Complete Execution Flow Diagram

```
git push to any branch
│
├─→ Job: build-test (ubuntu-latest - GitHub VM #1)
│   ├─ Step 1: Checkout
│   ├─ Step 2: Set up JDK 25
│   └─ Step 3: Maven install
│       │
│       ├─ Success → Continue
│       └─ Failure → Job marked "failed"
│
└─→ Job: docker-publish (needs: build-test, ubuntu-latest - GitHub VM #2)
    ├─ IF build-test succeeded:
    │   ├─ Step 1: Checkout (fresh repo)
    │   ├─ Step 2: Maven package
    │   ├─ Step 3: Docker login
    │   ├─ Step 4: Build image
    │   └─ Step 5: Push image
    │
    └─ IF build-test failed:
        └─ Job is skipped entirely
```

---

## 12. Practical Example: Handling Failures

```yaml
docker-publish:
  needs: build-test
  if: always()                    # Run regardless
  runs-on: ubuntu-latest
  steps:
    - name: Checkout
      run: git checkout
      if: always()                # Always run
      
    - name: Deploy on success
      run: docker push
      if: needs.build-test.result == 'success'
      
    - name: Notify on failure
      run: echo "Build failed"
      if: needs.build-test.result == 'failure'
      continue-on-error: true     # Notification failing doesn't matter
```

---

## Summary Table

| Aspect | Answer |
|--------|--------|
| Job default execution | Parallel |
| Step default execution | Sequential |
| `&` in step | Runs command in background |
| `wait` in step | Waits for all background jobs |
| Different jobs, same `runs-on` | Different VM instances |
| Share data between jobs | Use `actions/upload/download-artifact` |
| Step failure customizable | Yes, with `continue-on-error` |
| Job skip behavior | Default, but overridable with `if` |
| `if: success()` scope | Checks previous steps in same job (or use `needs.job.result`) |
| Default `runs-on` | GitHub-hosted VM (e.g., ubuntu-latest) |
| `self-hosted` | Your own runner machine |
| Workflow final status | Green (all pass), Yellow (errors but continued), Red (critical failure) |