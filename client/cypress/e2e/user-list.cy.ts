import { UserListPage } from '../support/user-list.po';

const page = new UserListPage();

describe('User list', () => {

  before(() => {
    cy.task('seed:database');
  });

  beforeEach(() => {
    page.navigateTo();
  });

  it('Should have the correct title', () => {
    page.getUserTitle().should('have.text', 'Users');
  });

  it('Should show 10 users', () => {
    page.getVisibleUsers().should('have.length', 10);
  });

  it('Should type something in the name filter and check that it returned correct elements', () => {
    // Filter for user 'Lynn Ferguson'
    page.filterByName('Lynn Ferguson');

    // All of the user cards should have the name we are filtering by
    page.getUserNames().each(userName => {
      cy.wrap(userName).should('have.text', 'Lynn Ferguson');
    });

    // (We check this two ways to show multiple ways to check this)
    page.getUserNames().each(userName =>
      expect(userName.text()).to.equal('Lynn Ferguson')
    );
  });

  it('Should be able to filter by age 27 and check that it returned correct elements', () => {
    // Filter for users of age '27'
    page.filterByAge(27);

    page.getVisibleUsers().should('have.lengthOf', 3);

    // Go through each of the visible users that are being shown and get the names
    console.log(page.getUserNames());
    page.getUserNames()
      // We should see these users whose age is 27
      .should('contain.text', 'Stokes Clayton')
      .should('contain.text', 'Bolton Monroe')
      .should('contain.text', 'Merrill Parker')
      // We shouldn't see these users
      .should('not.contain.text', 'Connie Stewart')
      .should('not.contain.text', 'Lynn Ferguson');
  });

  it('Should type something in the company filter and check that it returned correct elements', () => {
    // Filter for company 'OHMNET'
    page.filterByCompany('OHMNET');

    page.getVisibleUsers().should('have.lengthOf', 2);

    // All of the visible users should have the company we are filtering by
    page.getCompanyNames().each(companyName => {
      cy.wrap(companyName).should('have.text', 'OHMNET');
    });
  });

  it('Should type something partial in the company filter and check that it returned correct elements', () => {
    // Filter for companies that contain 'ti'
    page.filterByCompany('ti');

    page.getVisibleUsers().should('have.lengthOf', 2);

    // Each user card's company name should include the text we are filtering by
    page.getCompanyNames().each(companyName => {
      cy.wrap(companyName).should('include.text', 'TI');
    });
  });
});
