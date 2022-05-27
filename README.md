# :rescue_worker_helmet: FineAuthKit (In Progress)
A highly efficient and Easy-to-use Authentication toolkit library for android. It can handle complex tasks and even validate patterns and textinputs. We currently support Firebase and Back4App (Parse) for third-party-integrations like signup, login etc. You can make a request for support for other third-party service.

FineAuthKit supports password, email validation, sign integration, login integration, strong password generation etc. For knowing what operations are supported, read [Operations Supported page]() from documentation.

# Latest Release 
**Release `1.0.0` would be available soon.**

For now you can use `master-SNAPSHOT` versions.

# Implementation 

### Using Gradle
#### Step 1. Add the jitpack to your build file

```gradle
allprojects {
    repositories {
        //Add this block
        maven { url 'https://jitpack.io' }
    }
}
```

#### Step 2. Add the dependency to your build file

```gradle
dependencies { 	 
  implementation 'com.github.AdityaBavadekar:Fineauthkit:master-SNAPSHOT' 	
}
```

#### Step 3. Sync the project and you're ready to go


# Compatibility
- Minimum Android SDK: API level of 19 (Android 4.4 KitKat)

# How do I use FineAuthKit?
**Checkout our [Wiki]() for usage, examples and information. It would be updated after every release.**

Simple use case
```kotlin
    FineAuthKit.Verifier.isValidEmailAddress("example@gmail.com")
    FineAuthKit.GooglePhoneHintKit(fragmentActivity,listener).requestPhone()             
```

# Contributing 
We are always welcome to contributions, suggestions and problem solving.

# Author
[@Aditya Bavadekar](https://github.com/AdityaBavadekar) on GitHub

# License 
```

   Copyright 2022 Aditya Bavadekar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
