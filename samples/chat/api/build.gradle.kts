/*
 * Copyright 2015-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val rsocketVersion: String by rootProject
val kotlinxSerializationVersion: String by rootProject

kotlin {
    jvm()
    js {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api("io.rsocket.kotlin:rsocket-core:$rsocketVersion")
            api("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
        }
    }
}
