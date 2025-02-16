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

  it('Should be able to filter by age 27 and check that it returned correct elements', () => {
    // Filter for users of age '27'
    page.filterByAge(27);

    page.getVisibleUsers().should('have.lengthOf', 3);

    // Go through each of the visible users that are being shown and get the names
    page.getUserNames()
      // We should see these users whose age is 27
      .should('contain.text', 'Stokes Clayton')
      .should('contain.text', 'Bolton Monroe')
      .should('contain.text', 'Merrill Parker')
      // We shouldn't see these users
      .should('not.contain.text', 'Connie Stewart')
      .should('not.contain.text', 'Lynn Ferguson');
  });
});
