package org.simple.clinic.home.overdue.search

interface OverdueSearchUi {
  fun showSearchHistory(searchHistory: Set<String>)
  fun hideSearchResults()
  fun hideSearchHistory()
  fun renderSearchQuery(searchQuery: String)
}