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
});
