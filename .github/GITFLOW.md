# Simplified Gitflow with JReleaser and Entur Shared Workflow

This project uses a **simplified Gitflow model** with automated Maven Central publishing via **JReleaser** and the **Entur shared workflow**.

## Branch Structure

```
master (development)    → Active development with SNAPSHOT versions
  └─ 2.0.17-SNAPSHOT

release/*               → Release preparation and tags
  ├─ release/2.0.16 → tagged as v2.0.16
  └─ release/2.0.17 → tagged as v2.0.17

hotfix/*                → Urgent production fixes
  └─ hotfix/2.0.16.1 → tagged as v2.0.16.1

feature/*               → Feature development (optional)
  └─ feature/new-feature
```

## Comparison with gitflow-maven-plugin

| gitflow-maven-plugin | This Solution |
|---------------------|---------------|
| `mvn gitflow:release-start` | GitHub Actions → "Release Start (Gitflow)" |
| `mvn gitflow:release-finish` | GitHub Actions → "Release Finish (Gitflow)" (manual) |
| `mvn gitflow:hotfix-start` | GitHub Actions → "Hotfix Start (Gitflow)" |
| `mvn gitflow:hotfix-finish` | GitHub Actions → "Hotfix Finish (Gitflow)" (manual) |
| Maven plugin | JReleaser + Entur shared workflow |
| develop branch | master branch |

## Key Differences from Traditional Gitflow

- ✅ **No separate develop branch** - master serves as development branch
- ✅ **Simpler workflow** - Fewer branch merges
- ✅ **SNAPSHOT versions on master** - Always ready for development
- ✅ **Release branches** - Only for release preparation and tagging
- ✅ **No merge back needed** - Release branches are independent

---

## Workflows

### 1. Normal Development (Feature → Master)

**Daily development workflow:**

```bash
# Create feature branch from master (optional)
git checkout master
git pull
git checkout -b feature/my-feature

# Work on your feature
git add .
git commit -m "Add new feature"
git push origin feature/my-feature

# Create PR to master
# After merge, snapshot is automatically published to Maven Central
```

**What happens:**
- ✅ PR merged to `master`
- ✅ CI/CD runs (build, test, Sonar)
- ✅ Snapshot published to Maven Central: `X.Y.Z-SNAPSHOT`

---

### 2. Creating a Release

#### Step 1: Start Release

**Via GitHub Actions UI:**
1. Go to **Actions** → **"Release Start (Gitflow)"**
2. Click **"Run workflow"**
3. **Inputs:**
   - **Release version:** `2.0.17` (or leave empty to auto-remove SNAPSHOT)
   - **Base branch:** `master` (default)
4. Click **"Run workflow"**

**What happens:**
1. ✅ Creates `release/2.0.17` branch from `master`
2. ✅ Updates `pom.xml` version: `2.0.17-SNAPSHOT` → `2.0.17`
3. ✅ Commits and pushes the release branch

#### Step 2: Review & Test Release (Optional)

```bash
# Checkout release branch if you need to make changes
git checkout release/2.0.17

# Make final adjustments if needed
git add .
git commit -m "Final release adjustments"
git push
```

#### Step 3: Finish Release

**Via GitHub Actions UI:**
1. Go to **Actions** → **"Release Finish (Gitflow)"**
2. Click **"Run workflow"**
3. **Inputs:**
   - **Release branch:** `release/2.0.17`
   - **Next version for master:** `2.0.18-SNAPSHOT` (auto or manual)
   - **Next version increment:** `minor` (or `major`, `patch`)
4. Click **"Run workflow"**

**What happens automatically:**
1. ✅ Creates tag: `v2.0.17` from release branch
2. ✅ Publishes release `2.0.17` to **Maven Central**
3. ✅ Updates `master` to next version: `2.0.18-SNAPSHOT`
4. ✅ Deletes `release/2.0.17` branch

**Result:**
- 🎉 Release `2.0.17` published to Maven Central
- 🎉 Tag `v2.0.17` created
- 🎉 `master` ready for next iteration with `2.0.18-SNAPSHOT`

---

### 3. Creating a Hotfix

#### Step 1: Start Hotfix

**Via GitHub Actions UI:**
1. Go to **Actions** → **"Hotfix Start (Gitflow)"**
2. Click **"Run workflow"**
3. **Inputs:**
   - **Hotfix version:** `2.0.16.1` (or leave empty for auto: `2.0.16` → `2.0.16.1`)
   - **Base tag:** `v2.0.16` (the version to patch)
4. Click **"Run workflow"**

**What happens:**
1. ✅ Checks out from tag `v2.0.16`
2. ✅ Creates `hotfix/2.0.16.1` branch
3. ✅ Updates `pom.xml` version: `2.0.16` → `2.0.16.1`
4. ✅ Commits and pushes the hotfix branch

#### Step 2: Make Hotfix Changes

```bash
# Checkout hotfix branch
git checkout hotfix/2.0.16.1

# Fix the critical bug
git add .
git commit -m "Fix critical security issue"
git push
```

#### Step 3: Finish Hotfix

**Via GitHub Actions UI:**
1. Go to **Actions** → **"Hotfix Finish (Gitflow)"**
2. Click **"Run workflow"**
3. **Inputs:**
   - **Hotfix branch:** `hotfix/2.0.16.1`
   - **Merge back to master:** `true` (default - cherry-picks the fix)
4. Click **"Run workflow"**

**What happens automatically:**
1. ✅ Creates tag: `v2.0.16.1` from hotfix branch
2. ✅ Publishes hotfix `2.0.16.1` to **Maven Central**
3. ✅ Cherry-picks hotfix commits to `master` (optional)
4. ✅ Deletes `hotfix/2.0.16.1` branch

**Result:**
- 🎉 Hotfix `2.0.16.1` published to Maven Central
- 🎉 Tag `v2.0.16.1` created
- 🎉 `master` includes the hotfix (if merge back enabled)

---

## Version Management

### Master Branch
- Always has **SNAPSHOT** version (e.g., `2.0.17-SNAPSHOT`)
- Automatically incremented after each release

### Release Branches
- Created from master with SNAPSHOT removed
- Example: `2.0.17-SNAPSHOT` → `2.0.17`
- Tagged and published from release branch
- Deleted after successful release

### Hotfix Branches
- Created from release tags
- Increment patch or add 4th segment
- Example: `2.0.16` → `2.0.16.1`
- Tagged and published from hotfix branch

### Automatic Version Incrementing

After release, master is updated:
- **minor** (default): `2.0.17` → `2.0.18-SNAPSHOT`
- **major**: `2.0.17` → `3.0.0-SNAPSHOT`
- **patch**: `2.0.17` → `2.0.18-SNAPSHOT` (same as minor)
- **custom**: Specify exact version

---

## Workflow Diagram

```
RELEASE FLOW:

┌──────────────────────────────────────────────────────────────────┐
│                        MASTER BRANCH                             │
│                     (2.0.17-SNAPSHOT)                           │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ Release Start (GitHub Action)
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                   RELEASE/2.0.17 BRANCH                          │
│                        (2.0.17)                                  │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ Release Finish (GitHub Action)
               │ - Tag v2.0.17
               │ - Publish to Maven Central
               ↓
               🎉 PUBLISHED TO MAVEN CENTRAL

               Release Finish also updates master:
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                        MASTER BRANCH                             │
│                     (2.0.18-SNAPSHOT)                           │
└──────────────────────────────────────────────────────────────────┘


HOTFIX FLOW:

┌──────────────────────────────────────────────────────────────────┐
│                    TAG v2.0.16 (released)                        │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ Hotfix Start (GitHub Action)
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                  HOTFIX/2.0.16.1 BRANCH                          │
│                       (2.0.16.1)                                 │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ Hotfix Finish (GitHub Action)
               │ - Tag v2.0.16.1
               │ - Publish to Maven Central
               │ - Cherry-pick to master
               ↓
               🎉 PUBLISHED TO MAVEN CENTRAL

               Hotfix commits cherry-picked to master:
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                        MASTER BRANCH                             │
│          (2.0.18-SNAPSHOT + hotfix commits)                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

### Starting a Release
```bash
Actions → "Release Start (Gitflow)" → Run workflow
  → release_version: (empty for auto or specify version)
```

### Finishing a Release
```bash
Actions → "Release Finish (Gitflow)" → Run workflow
  → version: (release version to finish)
  → next_version_increment: minor | major | patch
```

### Starting a Hotfix
```bash
Actions → "Hotfix Start (Gitflow)" → Run workflow
  → hotfix_version: 2.0.16.1
  → base_tag: v2.0.16
```

### Finishing a Hotfix
```bash
Actions → "Hotfix Finish (Gitflow)" → Run workflow
  → version: 2.0.16.1
  → merge_to_master: true
```

---

## Benefits over gitflow-maven-plugin

1. ✅ **Simpler than traditional Gitflow** - No separate develop branch
2. ✅ **No local Maven plugin required** - Everything in GitHub Actions
3. ✅ **Better visibility** - All operations visible in GitHub UI
4. ✅ **Automated publishing** - JReleaser + Entur shared workflow
5. ✅ **Team collaboration** - No need for special local setup
6. ✅ **CI/CD native** - Built for cloud-native workflows
7. ✅ **Master always buildable** - SNAPSHOT versions always work

---

## Required Secrets

Ensure these secrets are configured in GitHub repository settings:

- `JRELEASER_NEXUS2_USERNAME` or `JRELEASER_SONATYPE_USERNAME`
- `JRELEASER_NEXUS2_PASSWORD` or `JRELEASER_SONATYPE_PASSWORD`
- `JRELEASER_GPG_PUBLIC_KEY`
- `JRELEASER_GPG_SECRET_KEY`
- `JRELEASER_GPG_PASSPHRASE`
- `JRELEASER_GITHUB_TOKEN` (inherited from `GITHUB_TOKEN`)
- `ENTUR_SONAR_PASSWORD` (for Sonar scans)

---

## Troubleshooting

### Manual Release from Tag

If automatic workflows fail, you can always manually trigger release:

```bash
Actions → "Release to Maven Central" (original workflow) → Run workflow
  → git_ref: v2.0.17
  → push_to_repo: false
```

### Cherry-pick Hotfix Manually

If automatic cherry-pick fails:

```bash
git checkout master
git cherry-pick <hotfix-commit-sha>
git push origin master
```

---

## Support

For issues or questions:
- Check workflow logs in GitHub Actions
- Review this documentation
- Check [Entur gha-maven-central](https://github.com/entur/gha-maven-central) documentation
- Check [JReleaser](https://jreleaser.org/) documentation