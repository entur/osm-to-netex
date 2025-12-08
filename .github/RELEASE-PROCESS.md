# Release Process - Gitflow with JReleaser

This document describes the complete release process for `netex-java-model` using a simplified Gitflow model with JReleaser and the Entur shared workflow.

## Overview

This solution replaces the traditional **gitflow-maven-plugin** approach with GitHub Actions workflows while maintaining similar functionality:

- ✅ **No Maven plugin required** - Everything automated via GitHub Actions
- ✅ **Master as development branch** - No separate develop branch needed
- ✅ **JReleaser + Entur shared workflow** - Automated publishing to Maven Central
- ✅ **Manual workflow triggers** - Full control over release process
- ✅ **Version management scripts** - Automated version incrementing

## Workflow Files

### Created Workflows

| File | Purpose | Trigger |
|------|---------|---------|
| `.github/workflows/deploy.yml` | Build, test, publish SNAPSHOT from master | Push/PR to master |
| `.github/workflows/release-start.yml` | Create release branch from master | Manual |
| `.github/workflows/release-finish.yml` | Tag, publish, update master | Manual |
| `.github/workflows/hotfix-start.yml` | Create hotfix branch from tag | Manual |
| `.github/workflows/hotfix-finish.yml` | Tag, publish hotfix, cherry-pick to master | Manual |
| `.github/workflows/release.yml` | Fallback manual release (legacy) | Tag push or manual |
| `.github/workflows/release-hotfix.yml` | Fallback manual hotfix (legacy) | Tag push or manual |

### Helper Scripts

| File | Purpose |
|------|---------|
| `.github/scripts/update-version.sh` | Update version in pom.xml using Maven |
| `.github/scripts/next-version.sh` | Calculate next version (major/minor/patch) |

## Complete Release Workflow

###1. Normal Development (Master Branch)

```
master (2.0.17-SNAPSHOT)
  ├─ feature/new-feature
  ├─ bugfix/issue-123
  └─ ... continuous development
```

**Process:**
```bash
# Develop on master or feature branches
git checkout -b feature/my-feature
git add .
git commit -m "Add new feature"
git push

# PR to master → CI runs → SNAPSHOT published to Maven Central
```

---

### 2. Release Process

#### Step 1: Start Release

**GitHub Actions → "Release Start (Gitflow)" → Run workflow**

**Inputs:**
- Release version: `2.0.17` (or leave empty)
- Base branch: `master`

**Result:**
- Creates `release/2.0.17` branch
- Updates `pom.xml`: `2.0.17-SNAPSHOT` → `2.0.17`

#### Step 2: Test Release Branch (Optional)

```bash
git checkout release/2.0.17
# Make any final adjustments
git push
```

#### Step 3: Finish Release

**GitHub Actions → "Release Finish (Gitflow)" → Run workflow**

**Inputs:**
- Release branch: `release/2.0.17`
- Next version increment: `minor` (default)
- Next version: (or specify exact like `2.1.0-SNAPSHOT`)

**Result:**
1. ✅ Tags `v2.0.17` from release branch
2. ✅ Publishes `2.0.17` to Maven Central
3. ✅ Updates master to `2.0.18-SNAPSHOT` (or `2.1.0-SNAPSHOT`)
4. ✅ Deletes `release/2.0.17` branch

---

### 3. Hotfix Process

#### Step 1: Start Hotfix

**GitHub Actions → "Hotfix Start (Gitflow)" → Run workflow**

**Inputs:**
- Hotfix version: `2.0.16.1` (or leave empty for auto)
- Base tag: `v2.0.16`

**Result:**
- Creates `hotfix/2.0.16.1` branch from tag `v2.0.16`
- Updates `pom.xml`: `2.0.16` → `2.0.16.1`

#### Step 2: Fix the Bug

```bash
git checkout hotfix/2.0.16.1
# Make your fix
git add .
git commit -m "Fix critical security issue"
git push
```

#### Step 3: Finish Hotfix

**GitHub Actions → "Hotfix Finish (Gitflow)" → Run workflow**

**Inputs:**
- Hotfix branch: `hotfix/2.0.16.1`
- Merge to master: `true` (cherry-pick hotfix to master)

**Result:**
1. ✅ Tags `v2.0.16.1` from hotfix branch
2. ✅ Publishes `2.0.16.1` to Maven Central
3. ✅ Cherry-picks hotfix commits to master
4. ✅ Deletes `hotfix/2.0.16.1` branch

---

## Comparison with gitflow-maven-plugin

| gitflow-maven-plugin | This Solution |
|---------------------|---------------|
| `mvn gitflow:release-start -DreleaseVersion=2.0.17` | GitHub Actions → Release Start |
| `mvn gitflow:release-finish -DdevelopmentVersion=2.0.18-SNAPSHOT` | GitHub Actions → Release Finish |
| `mvn gitflow:hotfix-start -DhotfixVersion=2.0.16.1` | GitHub Actions → Hotfix Start |
| `mvn gitflow:hotfix-finish` | GitHub Actions → Hotfix Finish |
| Local Maven execution | Cloud-based GitHub Actions |
| develop branch | master branch (simplified) |
| Manual Maven commands | Automated workflows |
| No CI/CD integration | Full CI/CD with Maven Central publishing |

---

## Version Management

### Master Branch
- Always contains **SNAPSHOT** version (e.g., `2.0.17-SNAPSHOT`)
- Auto-incremented after releases
- Continuous integration with snapshot publishing

### Release Branches (`release/*`)
- Created from master
- Version without SNAPSHOT (e.g., `2.0.17`)
- Tagged and published
- Deleted after release

### Hotfix Branches (`hotfix/*`)
- Created from release tags
- 4-segment version (e.g., `2.0.16.1`)
- Tagged and published
- Deleted after hotfix

### Version Increment Strategies

| Strategy | Example | Use Case |
|----------|---------|----------|
| **patch** | 2.0.16 → 2.0.17 | Bug fixes, small changes |
| **minor** | 2.0.16 → 2.1.0 | New features (default) |
| **major** | 2.0.16 → 3.0.0 | Breaking changes |
| **custom** | Specify exact version | Special cases |

---

## Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    MASTER BRANCH (Development)                   │
│                       2.0.17-SNAPSHOT                           │
│                                                                  │
│  ├─ feature/xyz                                                 │
│  ├─ bugfix/123                                                  │
│  └─ Continuous development                                      │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │ [1] Release Start → Creates release/2.0.17
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                    RELEASE/2.0.17 BRANCH                         │
│                          2.0.17                                  │
│                                                                  │
│  - Final testing                                                │
│  - Last-minute fixes                                            │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ [2] Release Finish
               │     - Tag v2.0.17
               │     - Publish to Maven Central
               │     - Update master to 2.0.18-SNAPSHOT
               │     - Delete release branch
               ↓
               🎉 PUBLISHED: v2.0.17


HOTFIX FLOW:

┌──────────────────────────────────────────────────────────────────┐
│                       TAG v2.0.16 (Production)                   │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ [1] Hotfix Start → Creates hotfix/2.0.16.1
               ↓
┌──────────────────────────────────────────────────────────────────┐
│                   HOTFIX/2.0.16.1 BRANCH                         │
│                        2.0.16.1                                  │
│                                                                  │
│  - Critical bug fix                                             │
└──────────────┬───────────────────────────────────────────────────┘
               │
               │ [2] Hotfix Finish
               │     - Tag v2.0.16.1
               │     - Publish to Maven Central
               │     - Cherry-pick to master
               │     - Delete hotfix branch
               ↓
               🎉 PUBLISHED: v2.0.16.1
```

---

## Quick Reference

### Release a New Version

1. **Actions** → **Release Start** → Enter version → Run
2. Review and test `release/X.Y.Z` branch
3. **Actions** → **Release Finish** → Enter release branch → Run
4. Done! Published to Maven Central, master updated

### Create a Hotfix

1. **Actions** → **Hotfix Start** → Enter version and base tag → Run
2. Fix bug on `hotfix/X.Y.Z.W` branch
3. **Actions** → **Hotfix Finish** → Enter hotfix branch → Run
4. Done! Published to Maven Central, fix merged to master

### Check Published Versions

- **Maven Central**: https://central.sonatype.com/artifact/org.entur/netex-java-model
- **GitHub Releases**: https://github.com/entur/netex-java-model/releases
- **Tags**: https://github.com/entur/netex-java-model/tags

---

## Secrets Required

Ensure these are configured in GitHub repository settings:

| Secret | Purpose |
|--------|---------|
| `JRELEASER_NEXUS2_USERNAME` | Maven Central username |
| `JRELEASER_NEXUS2_PASSWORD` | Maven Central password |
| `JRELEASER_GPG_PUBLIC_KEY` | GPG public key for signing |
| `JRELEASER_GPG_SECRET_KEY` | GPG secret key for signing |
| `JRELEASER_GPG_PASSPHRASE` | GPG passphrase |
| `ENTUR_SONAR_PASSWORD` | SonarCloud token |
| `GITHUB_TOKEN` | Auto-provided by GitHub Actions |

---

## Benefits

1. ✅ **Simpler than traditional Gitflow** - No separate develop branch
2. ✅ **No local tools required** - Everything in GitHub Actions
3. ✅ **Full automation** - From tag to Maven Central
4. ✅ **Team-friendly** - No special setup needed
5. ✅ **Audit trail** - All operations logged in GitHub
6. ✅ **Version control** - Automatic version management
7. ✅ **Safe releases** - Separate release branches for testing

---

## Troubleshooting

### Workflow fails

- Check GitHub Actions logs
- Ensure all secrets are configured
- Verify branch/tag names are correct

### Version conflicts

- Ensure master has SNAPSHOT version
- Release branches should not have SNAPSHOT
- Hotfix versions should be 4-segment (X.Y.Z.W)

### Manual fallback

If workflows fail, you can always use the legacy workflows:

```bash
Actions → "Release to Maven Central" → Run workflow
  git_ref: v2.0.17
  push_to_repo: false
```

---

## Support

- Documentation: [GITFLOW.md](.github/GITFLOW.md)
- Entur Workflow: https://github.com/entur/gha-maven-central
- JReleaser: https://jreleaser.org/