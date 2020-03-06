package com.mapswithme.maps.editor.data

/**
 * Class which contains array of localized names with following priority:
 * 1. Names for Mwm languages;
 * 2. User`s language name;
 * 3. Other names;
 * and mandatoryNamesCount - count of names which should be always shown.
 */
class NamesDataSource(val names: Array<LocalizedName>, val mandatoryNamesCount: Int)