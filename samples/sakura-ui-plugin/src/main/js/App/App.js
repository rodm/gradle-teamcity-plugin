
import {H2} from '@jetbrains/ring-ui/components/heading/heading'

import {React} from "@jetbrains/teamcity-api"
import type {PluginContext} from "@jetbrains/teamcity-api";

import styles from './App.css'

const defaultProfile = {
    name: "Elvis",
}

const ProfileInfo = ({onNameClick, firstName, lastName}) =>
  <H2 className={styles.name} onClick={onNameClick}>{`Hello, ${firstName} ${lastName ?? ''}`}</H2>

function App({location}: {| location: PluginContext |}) {
    const [expanded, setExpanded] = React.useState(false)
    const toggleExpanded = React.useCallback(() => setExpanded(state => !state), [])

    return (
        <div className={styles.wrapper}>
            <ProfileInfo onNameClick={toggleExpanded} firstName={defaultProfile.name} />
            {expanded && <div>
                {Object.entries(location).map(([key, value]) => value ? <p key={key}>{`${key}:${value}`}</p>: null)}
            </div>}
        </div>
    )
}

export default App
