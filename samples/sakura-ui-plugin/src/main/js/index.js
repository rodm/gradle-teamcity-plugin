// @flow strict

import {Plugin, React} from "@jetbrains/teamcity-api"
import App from './App/App'

new Plugin([Plugin.placeIds.SAKURA_SIDEBAR_TOP, Plugin.placeIds.BEFORE_CONTENT], {
    name: "Sakura UI Plugin",
    content: App,
});
