# Illumined Firebase configuration

`firestore.rules` is a locally validated, class-scoped replacement for the rules currently deployed to the shared `ocia-application` project.

## Validation

Compile without publishing:

```sh
firebase deploy --only firestore:rules --project ocia-application --dry-run --non-interactive
```

Run the local authorization suite:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  firebase emulators:exec --only firestore --project ocia-application --non-interactive \
  'npm --prefix firebase/rules-tests test'
```

## Production deployment

Publishing changes the security boundary used by iOS, Android, and the web app. Obtain explicit approval, then deploy only the rules:

```sh
firebase deploy --only firestore:rules --project ocia-application --non-interactive
```

After deployment, smoke-test a learner and an instructor from the same class. Also verify that an instructor cannot read or mutate a document whose `classId` belongs to another class.
