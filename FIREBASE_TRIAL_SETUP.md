# Firebase Trial Access Setup

## 1) Deploy Firestore Rules

From project root:

```bash
firebase deploy --only firestore:rules
```

This uses `firestore.rules` in this repo.

## 2) Deploy Cloud Functions

From project root:

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

Functions added:
- `initUserAccessOnSignup`
  - On Auth signup, creates/updates `users/{uid}` with:
    - `status = approved`
    - `planStatus = trial`
    - `trialStartAt`
    - `trialEndAt` (30 days)
- `expireTrialsAndSubscriptions`
  - Scheduled daily at 00:10 Asia/Kolkata
  - Marks overdue `trial` users as `planStatus = expired` and `status = pending`
  - Marks overdue `active` users as `planStatus = expired` and `status = pending`

## 3) Required Firestore Composite Indexes

For scheduled expiry queries, Firestore may prompt index creation for:
- `users`: `status`, `planStatus`, `trialEndAt`
- `users`: `status`, `planStatus`, `subscriptionEndAt`

Create indexes from the Firebase Console link shown in deploy/runtime logs.

## 4) Data Contract for App Access

Document: `users/{uid}`

Required fields:
- `status`: `approved | blocked | pending`
- `planStatus`: `trial | active | expired`
- `trialEndAt`: timestamp (required for trial)
- `subscriptionEndAt`: timestamp (required for active)

App access is granted only when:
- `status == approved`, and
- (`planStatus == trial` and `trialEndAt` is in future), or
- (`planStatus == active` and `subscriptionEndAt` is in future).
