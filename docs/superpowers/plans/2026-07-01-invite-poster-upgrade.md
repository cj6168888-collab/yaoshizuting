# Invite Poster Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing invite QR card into a conversion-focused promotional poster that follows the selected C visual direction.

**Architecture:** Keep the current `/user/invite-qr` API, `inviteQrData`, `inviteQrImage`, `copyInviteLink`, and bind-parent flows. Replace only the invite page layout and related CSS inside `frontend/src/App.vue`, using Vue state already loaded by `loadInviteQr()`.

**Tech Stack:** Vue 3 single-file app, qrcode data URL generation, existing CSS variables and Vite build.

---

### Task 1: Replace Invite Poster Markup

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] In the `activeMenu === 'invite'` section, replace the current single `.invite-block > .qr-card.share-card` with a two-column `.invite-upgrade-grid`.
- [ ] Keep `v-if="inviteQrData"` and the existing empty state.
- [ ] Add a left `.invite-poster-preview` containing:
  - `.invite-poster-alert`: `好友报名 · 到店加赠`
  - `.invite-poster-brand`: logo + `药师祖庭`
  - `.invite-poster-title`: `分享好友` and `一起领体验`
  - `.invite-poster-subtitle`: registration and binding explanation
  - `.invite-benefit-grid`: four benefit chips
  - `.invite-poster-qr-card`: QR image, scan instruction, inviter metadata
  - `.invite-poster-disclaimer`: compliance text
- [ ] Add a right `.invite-action-panel` containing:
  - title `让推广码真正帮你拓客`
  - three short action cards
  - invite link code block
  - buttons for copy, save tip, and team view
- [ ] Reuse existing Vue values:
  - `logoUrl`
  - `inviteQrImage`
  - `inviteQrData.parentNickname`
  - `inviteQrData.parentId`
  - `inviteQrData.inviteUrl`

### Task 2: Add Safe Helper For Friend View

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] Add a `viewInviteFriends()` function near `showPosterSaveTip()`.
- [ ] If the current user can view the team menu, call `switchMenu('team')`.
- [ ] Otherwise set `bindResult` to a non-error message: `已复制推广码后，可在团队列表查看后续绑定关系`.
- [ ] Update `showPosterSaveTip()` message to: `当前海报可直接截图保存；后续可接入一键生成图片`.

### Task 3: Add Poster CSS

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] Add CSS classes after the existing invite styles:
  - `.invite-upgrade-grid`
  - `.invite-poster-preview`
  - `.invite-poster-alert`
  - `.invite-poster-brand`
  - `.invite-poster-title`
  - `.invite-poster-subtitle`
  - `.invite-benefit-grid`
  - `.invite-poster-qr-card`
  - `.invite-qr-shell`
  - `.invite-poster-meta`
  - `.invite-poster-disclaimer`
  - `.invite-action-panel`
  - `.invite-action-list`
  - `.invite-action-item`
  - `.invite-link-box`
  - `.invite-action-buttons`
- [ ] Use the selected C direction:
  - teal-green gradient background
  - restrained red alert chip
  - warm gold title accents
  - white QR card with strong contrast
  - deep green brand area and action buttons
- [ ] Keep card radii at 8px where possible.
- [ ] Add responsive rules under the existing mobile media query so 390px width has one column, no overflow, and buttons stack cleanly.

### Task 4: Verify Locally

**Commands:**
- `npm run build` from `frontend`
- Browser smoke on `http://127.0.0.1:4000/`:
  - login as `admin/admincj`
  - open invite menu
  - verify `.invite-poster-preview` is visible
  - verify QR image exists
  - verify `document.documentElement.scrollWidth <= window.innerWidth` at 390px viewport

### Task 5: Clean Visual Companion State

**Files:**
- Modify: `.superpowers/brainstorm/invite-poster-20260701/content/*.html`

- [ ] Push a waiting screen to the visual companion so the browser no longer shows an unresolved design choice.
- [ ] Do not delete persisted brainstorming files; they are useful design records.
