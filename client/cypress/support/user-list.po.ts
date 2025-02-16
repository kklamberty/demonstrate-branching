import { UserRole } from 'src/app/users/user';

export class UserListPage {
  private readonly pageUrl = '/users';
  private readonly pageTitle = '[data-test=userListTitle]';
  private readonly user = '[data-test=user]';
  private readonly username = '[data-test=userName]';
  private readonly companyName = '[data-test=userCompany]';
  private readonly ageFilter = '[data-test=userAgeFilter]';
  private readonly companyFilter = '[data-test=userCompanyFilter]';
  private readonly userCardSelector = '.user-cards-container app-user-card';
  private readonly userListItemsSelector = '.user-nav-list .user-list-item';
  private readonly profileButtonSelector = '[data-test=viewProfileButton]';
  private readonly radioButtonSelector = '[data-test=viewTypeRadio] mat-radio-button';
  private readonly userRoleDropdownSelector = '[data-test=userRoleSelect]';
  private readonly dropdownOptionSelector = 'mat-option';
  private readonly addUserButtonSelector = '[data-test=addUserButton]';

  navigateTo() {
    return cy.visit(this.pageUrl);
  }

  /**
   * Gets the title of the app when visiting the `/users` page.
   *
   * @returns the value of the element with the [data-test=userListTitle]
   */
  getUserTitle() {
    return cy.get(this.pageTitle);
  }

  /**
   * Gets the collection of things that have the [data-test=user] attribute.
   *
   * @returns an iterable of the elements that have the [data-test=user] attribute
   */
  getVisibleUsers() {
    return cy.get(this.user);
  }

  /**
   * Gets the collection of things that have the [data-test=userName] attribute.
   *
   * @returns an iterable of the elements that have the [data-test=userName] attribute
   */
  getUserNames() {
    return cy.get(this.username);
  }

  /**
   * Gets the collection of things that have the [data-test=userCompany] attribute.
   *
   * @returns an iterable of the elements that have the [data-test=userCompany] attribute
   */
  getCompanyNames() {
    return cy.get(this.companyName);
  }

  /**
   * Causes the page to do what it needs to do to filter by selecting `number` in the
   * element that has the [data-test=userAgeFilter] attribute (in this case, by typing).
   *
   * @returns the result of cy.get the element with [data-test=userAgeFilter] attribute
   */
  filterByAge(age: number) {
    return cy.get(this.ageFilter).type(age.toString());
  }

  /**
   * Causes the page to do what it needs to do to filter by selecting `company` in the
   * element that has the [data-test=userCompanyFilter] attribute (in this case, by typing).
   *
   * @returns the result of cy.get the element with [data-test=userCompanyFilter] attribute
   */
  filterByCompany(company: string) {
    return cy.get(this.companyFilter).type(company);
  }

  /**
   * Get all the `app-user-card` DOM elements. This will be
   * empty if we're using the list view of the users.
   *
   * @returns an iterable (`Cypress.Chainable`) containing all
   *   the `app-user-card` DOM elements.
   */
   getUserCards() {
    return cy.get(this.userCardSelector);
  }

  /**
   * Get all the `.user-list-item` DOM elements. This will
   * be empty if we're using the card view of the users.
   *
   * @returns an iterable (`Cypress.Chainable`) containing all
   *   the `.user-list-item` DOM elements.
   */
  getUserListItems() {
    return cy.get(this.userListItemsSelector);
  }

  /**
   * Clicks the "view profile" button for the given user card.
   * Requires being in the "card" view.
   *
   * @param card The user card
   */
  clickViewProfile(card: Cypress.Chainable<JQuery<HTMLElement>>) {
    return card.find<HTMLButtonElement>(this.profileButtonSelector).click();
  }

  /**
   * Change the view of users.
   *
   * @param viewType Which view type to change to: "card" or "list".
   */
  changeView(viewType: 'card' | 'list') {
    return cy.get(`${this.radioButtonSelector}[value="${viewType}"]`).click();
  }

  /**
   * Selects a role to filter in the "Role" selector.
   *
   * @param value The role *value* to select, this is what's found in the mat-option "value" attribute.
   */
  selectRole(value: UserRole) {
    // Find and click the drop down
    cy.get(this.userRoleDropdownSelector).click();
    // Select and click the desired value from the resulting menu
    return cy.get(`${this.dropdownOptionSelector}[value="${value}"]`).click();
  }

  addUserButton() {
    return cy.get(this.addUserButtonSelector);
  }
}
